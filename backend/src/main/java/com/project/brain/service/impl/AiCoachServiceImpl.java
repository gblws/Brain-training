package com.project.brain.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.brain.cache.RedisCacheService;
import com.project.brain.dto.AiWeeklyReportResponse;
import com.project.brain.model.BaselineAssessment;
import com.project.brain.repository.BaselineAssessmentRepository;
import com.project.brain.service.AiCoachService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AiCoachServiceImpl implements AiCoachService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BaselineAssessmentRepository baselineAssessmentRepository;
    private final RedisCacheService redisCacheService;
    private final TaskExecutor aiReportExecutor;
    private final ObjectMapper objectMapper;
    private final Set<String> runningKeys = ConcurrentHashMap.newKeySet();

    @Value("${ai.deepseek.base-url:https://api.deepseek.com/v1}")
    private String deepseekBaseUrl;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String deepseekModel;

    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${ai.deepseek.timeout-seconds:40}")
    private Integer deepseekTimeoutSeconds;

    @Value("${ai.deepseek.max-tokens:500}")
    private Integer deepseekMaxTokens;

    @Value("${ai.report.cache-ttl-minutes:60}")
    private Integer reportCacheTtlMinutes;

    public AiCoachServiceImpl(
            BaselineAssessmentRepository baselineAssessmentRepository,
            RedisCacheService redisCacheService,
            @Qualifier("aiReportExecutor") TaskExecutor aiReportExecutor,
            ObjectMapper objectMapper) {
        this.baselineAssessmentRepository = baselineAssessmentRepository;
        this.redisCacheService = redisCacheService;
        this.aiReportExecutor = aiReportExecutor;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiWeeklyReportResponse getWeeklyReport(String subjectId, boolean refresh) {
        if (!StringUtils.hasText(subjectId)) {
            throw new IllegalArgumentException("subjectId is required");
        }

        String normalizedSubjectId = subjectId.trim();
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        String cacheKey = cacheKey(normalizedSubjectId, weekStart, weekEnd);

        if (refresh) {
            redisCacheService.delete(cacheKey);
        }

        AiWeeklyReportResponse cached = redisCacheService.get(cacheKey, AiWeeklyReportResponse.class);
        if (cached != null && !refresh) {
            cached.setStatus("ready");
            cached.setSource(StringUtils.hasText(cached.getSource()) ? cached.getSource() : "cache");
            return cached;
        }

        if (runningKeys.contains(cacheKey)) {
            return processingResponse(normalizedSubjectId, weekStart, weekEnd);
        }

        runningKeys.add(cacheKey);
        aiReportExecutor.execute(() -> generateAndCache(normalizedSubjectId, weekStart, weekEnd, cacheKey));
        return processingResponse(normalizedSubjectId, weekStart, weekEnd);
    }

    private void generateAndCache(String subjectId, LocalDate weekStart, LocalDate weekEnd, String cacheKey) {
        try {
            WeekStats currentWeekStats = calculateWeekStats(subjectId, weekStart, weekEnd);
            WeekStats previousWeekStats = calculateWeekStats(subjectId, weekStart.minusWeeks(1), weekEnd.minusWeeks(1));
            boolean useDemoData = currentWeekStats.count <= 0;
            if (useDemoData) {
                currentWeekStats = buildDemoWeekStats(subjectId, weekStart, false);
                if (previousWeekStats.count <= 0) {
                    previousWeekStats = buildDemoWeekStats(subjectId, weekStart.minusWeeks(1), true);
                }
            }

            AiWeeklyReportResponse response;
            if (!StringUtils.hasText(deepseekApiKey)) {
                response = buildFallbackResponse(subjectId, weekStart, weekEnd, currentWeekStats, previousWeekStats,
                        "fallback");
            } else {
                String prompt = buildPrompt(subjectId, weekStart, weekEnd, currentWeekStats, previousWeekStats);
                String modelRaw = callDeepSeek(prompt);
                response = buildAiResponse(subjectId, weekStart, weekEnd, currentWeekStats, previousWeekStats,
                        modelRaw);
            }
            if (useDemoData) {
                markDemoResponse(response);
            }

            Duration ttl = Duration.ofMinutes(Math.max(5, reportCacheTtlMinutes == null ? 60 : reportCacheTtlMinutes));
            redisCacheService.set(cacheKey, response, ttl);
        } catch (Exception exception) {
            AiWeeklyReportResponse fallback = buildFallbackResponse(
                    subjectId,
                    weekStart,
                    weekEnd,
                    calculateWeekStatsSafe(subjectId, weekStart, weekEnd),
                    calculateWeekStatsSafe(subjectId, weekStart.minusWeeks(1), weekEnd.minusWeeks(1)),
                    "fallback");
            fallback.setTrend("AI 生成失败，已返回兜底报告。原因: " + exception.getMessage());
            redisCacheService.set(cacheKey, fallback, Duration.ofMinutes(10));
        } finally {
            runningKeys.remove(cacheKey);
        }
    }

    private WeekStats calculateWeekStatsSafe(String subjectId, LocalDate start, LocalDate end) {
        try {
            return calculateWeekStats(subjectId, start, end);
        } catch (Exception ignore) {
            return new WeekStats();
        }
    }

    private WeekStats calculateWeekStats(String subjectId, LocalDate start, LocalDate end) {
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.atTime(LocalTime.MAX);

        List<BaselineAssessment> history = baselineAssessmentRepository
                .findAllBySubjectIdOrderByCreateTimeDesc(subjectId)
                .stream()
                .filter(item -> item.getCreateTime() != null
                        && !item.getCreateTime().isBefore(startTime)
                        && !item.getCreateTime().isAfter(endTime))
                .collect(Collectors.toList());

        WeekStats stats = new WeekStats();
        stats.count = history.size();
        if (history.isEmpty()) {
            return stats;
        }

        double observation = history.stream().mapToDouble(item -> nvl(item.getObservationScore())).average().orElse(0D);
        double memory = history.stream().mapToDouble(item -> nvl(item.getMemoryDimensionScore())).average().orElse(0D);
        double spatial = history.stream().mapToDouble(item -> nvl(item.getSpatialScore())).average().orElse(0D);
        double calculation = history.stream().mapToDouble(item -> nvl(item.getCalculationScore())).average().orElse(0D);
        double reasoning = history.stream().mapToDouble(item -> nvl(item.getReasoningScore())).average().orElse(0D);
        double creativity = history.stream().mapToDouble(item -> nvl(item.getCreativityScore())).average().orElse(0D);

        stats.observation = round2(observation);
        stats.memory = round2(memory);
        stats.spatial = round2(spatial);
        stats.calculation = round2(calculation);
        stats.reasoning = round2(reasoning);
        stats.creativity = round2(creativity);
        stats.overall = round2((observation + memory + spatial + calculation + reasoning + creativity) / 6D);
        return stats;
    }

    private String buildPrompt(
            String subjectId,
            LocalDate weekStart,
            LocalDate weekEnd,
            WeekStats current,
            WeekStats previous) {
        String cleanedText = buildCleanedText(subjectId, weekStart, weekEnd, current, previous);

        return "你是一名严谨、直接、但不刻薄的认知训练分析师。" +
                "你的任务不是鼓励用户，而是基于给定测评数据做简洁、具体、可执行的分析。" +
                "请只返回一个 JSON 对象，不要 markdown，不要解释，不要添加任何额外字段。" +
                "JSON 键固定为：summary, trend, strengths, risks, suggestions。" +
                "要求如下：" +
                "1.summary 用 2~3 句，总结本周整体状态，必须引用给定数据，不要说空话；" +
                "2.trend 用 1~2 句，对比上周变化；除非本周样本数为0，否则必须基于现有数据直接判断，不要说“样本不足无法判断”；" +
                "3.strengths/risk/suggestions 均为数组，每个数组 2~4 条；" +
                "4.优势和劣势必须优先围绕维度高低、波动和样本量展开；" +
                "5.训练建议必须直接落到游戏层面，明确说明哪个游戏作为主训练、哪个游戏作为辅助训练；" +
                "6.如果创造力维度没有真实测量依据，必须把它视为“未充分测量”，不要假装分析；" +
                "7.禁止使用“数据空白提供机会”“重新开始也很好”这类模板句；" +
                "8.如果本周样本数为 0，直接说明数据不足，请进行测评。\n\n" +
                cleanedText;
    }

    private String buildCleanedText(
            String subjectId,
            LocalDate weekStart,
            LocalDate weekEnd,
            WeekStats current,
            WeekStats previous) {
        Map<String, Double> currentDimensions = buildDimensionMap(current);
        Map<String, Double> previousDimensions = buildDimensionMap(previous);
        List<Map.Entry<String, Double>> sorted = currentDimensions.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());

        String strongest = sorted.isEmpty() ? "无" : sorted.get(0).getKey() + "=" + sorted.get(0).getValue();
        String weakest = sorted.isEmpty() ? "无"
                : sorted.get(sorted.size() - 1).getKey() + "=" + sorted.get(sorted.size() - 1).getValue();

        return "用户ID: " + subjectId + "\n" +
                "统计周期: " + weekStart.format(DATE_FORMATTER) + " ~ " + weekEnd.format(DATE_FORMATTER) + "\n" +
                "本周样本数: " + current.count + "\n" +
                "上周样本数: " + previous.count + "\n" +
                "本周综合分: " + current.overall + "\n" +
                "上周综合分: " + previous.overall + "\n" +
                "综合分变化: " + round2(current.overall - previous.overall) + "\n" +
                "本周六维分: 观察力=" + current.observation +
                ", 记忆力=" + current.memory +
                ", 空间力=" + current.spatial +
                ", 计算力=" + current.calculation +
                ", 推理力=" + current.reasoning +
                ", 创造力=" + current.creativity + "\n" +
                "上周六维分: 观察力=" + previous.observation +
                ", 记忆力=" + previous.memory +
                ", 空间力=" + previous.spatial +
                ", 计算力=" + previous.calculation +
                ", 推理力=" + previous.reasoning +
                ", 创造力=" + previous.creativity + "\n" +
                "维度变化: 观察力=" + round2(current.observation - previous.observation) +
                ", 记忆力=" + round2(current.memory - previous.memory) +
                ", 空间力=" + round2(current.spatial - previous.spatial) +
                ", 计算力=" + round2(current.calculation - previous.calculation) +
                ", 推理力=" + round2(current.reasoning - previous.reasoning) +
                ", 创造力=" + round2(current.creativity - previous.creativity) + "\n" +
                "本周最高维度: " + strongest + "\n" +
                "本周最低维度: " + weakest + "\n" +
                "维度说明: 创造力当前多数情况下没有独立游戏支撑，缺少样本时请谨慎表述。";
    }

    private String callDeepSeek(String prompt) {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(deepseekApiKey)
                .baseUrl(deepseekBaseUrl)
                .modelName(deepseekModel)
                .temperature(0.2)
                .maxTokens(Math.max(128, deepseekMaxTokens == null ? 500 : deepseekMaxTokens))
                .timeout(Duration.ofSeconds(Math.max(10, deepseekTimeoutSeconds == null ? 40 : deepseekTimeoutSeconds)))
                .build();
        return model.chat(prompt);
    }

    private AiWeeklyReportResponse buildAiResponse(
            String subjectId,
            LocalDate weekStart,
            LocalDate weekEnd,
            WeekStats current,
            WeekStats previous,
            String modelRaw) {
        AiModelOutput parsed = parseModelOutput(modelRaw);

        AiWeeklyReportResponse response = new AiWeeklyReportResponse();
        response.setStatus("ready");
        response.setSource("ai");
        response.setSubjectId(subjectId);
        response.setWeekStart(weekStart.format(DATE_FORMATTER));
        response.setWeekEnd(weekEnd.format(DATE_FORMATTER));
        response.setSummary(defaultText(parsed.summary, buildDefaultSummary(current)));
        response.setTrend(defaultText(parsed.trend, buildDefaultTrend(current, previous)));
        response.setStrengths(normalizeList(parsed.strengths, buildDefaultStrengths(current)));
        response.setRisks(normalizeList(parsed.risks, buildDefaultRisks(current, previous)));
        response.setSuggestions(
                normalizeList(parsed.suggestions, buildDefaultSuggestions(current)));
        response.setGeneratedAt(LocalDateTime.now().format(DATETIME_FORMATTER));
        return response;
    }

    private AiWeeklyReportResponse buildFallbackResponse(
            String subjectId,
            LocalDate weekStart,
            LocalDate weekEnd,
            WeekStats current,
            WeekStats previous,
            String source) {
        AiWeeklyReportResponse response = new AiWeeklyReportResponse();
        response.setStatus("ready");
        response.setSource(source);
        response.setSubjectId(subjectId);
        response.setWeekStart(weekStart.format(DATE_FORMATTER));
        response.setWeekEnd(weekEnd.format(DATE_FORMATTER));

        double delta = round2(current.overall - previous.overall);
        response.setSummary(
                current.count <= 0
                        ? "本周暂无有效基准测评数据，请先完成至少一次基准测试。"
                        : "本周完成 " + current.count + " 次基准测试，综合分 " + current.overall + "。");
        response.setTrend("相比上周: " + (delta >= 0 ? "+" : "") + delta + " 分。");

        Map<String, Double> dimensionMap = new LinkedHashMap<>();
        dimensionMap.put("Observation", current.observation);
        dimensionMap.put("Memory", current.memory);
        dimensionMap.put("Spatial", current.spatial);
        dimensionMap.put("Calculation", current.calculation);
        dimensionMap.put("Reasoning", current.reasoning);
        dimensionMap.put("Creativity", current.creativity);

        List<Map.Entry<String, Double>> sorted = dimensionMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());

        List<String> strengths = new ArrayList<>();
        if (!sorted.isEmpty()) {
            strengths.add(sorted.get(0).getKey() + "是当前最稳定的优势维度。");
        }
        if (sorted.size() > 1) {
            strengths.add(sorted.get(1).getKey() + "维度表现保持稳定。");
        }
        if (strengths.isEmpty()) {
            strengths.add("持续完成基准测试后才能形成可靠趋势。");
        }

        List<String> risks = new ArrayList<>();
        if (sorted.size() > 4) {
            risks.add(sorted.get(sorted.size() - 1).getKey() + "是当前主要短板。");
            risks.add(sorted.get(sorted.size() - 2).getKey() + "存在明显提升空间。");
        } else {
            risks.add("样本量偏少，趋势置信度有限。");
            risks.add("训练频次偏低，随机波动较大。");
        }

        List<String> suggestions = List.of(
                "下周至少安排 3 次训练，每次不少于 15 分钟。",
                "每次先进行最弱维度对应的训练游戏。",
                "每周固定复盘一次，并按结果调整难度。");

        response.setStrengths(strengths);
        response.setRisks(risks);
        response.setSuggestions(suggestions);
        response.setGeneratedAt(LocalDateTime.now().format(DATETIME_FORMATTER));
        return response;
    }

    private AiWeeklyReportResponse processingResponse(String subjectId, LocalDate weekStart, LocalDate weekEnd) {
        AiWeeklyReportResponse response = new AiWeeklyReportResponse();
        response.setStatus("processing");
        response.setSource("async");
        response.setSubjectId(subjectId);
        response.setWeekStart(weekStart.format(DATE_FORMATTER));
        response.setWeekEnd(weekEnd.format(DATE_FORMATTER));
        response.setSummary("AI 报告正在生成，请稍后刷新。");
        response.setTrend("");
        response.setGeneratedAt(LocalDateTime.now().format(DATETIME_FORMATTER));
        return response;
    }

    private AiModelOutput parseModelOutput(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new AiModelOutput();
        }
        String normalized = extractJson(raw);
        if (!StringUtils.hasText(normalized)) {
            return new AiModelOutput();
        }
        try {
            return objectMapper.readValue(normalized, AiModelOutput.class);
        } catch (Exception ignore) {
            return new AiModelOutput();
        }
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return raw.substring(start, end + 1);
    }

    private List<String> normalizeList(List<String> values, List<String> defaults) {
        List<String> list = values == null ? new ArrayList<>()
                : values.stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .distinct()
                        .limit(4)
                        .collect(Collectors.toList());
        return list.isEmpty() ? defaults : list;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String cacheKey(String subjectId, LocalDate weekStart, LocalDate weekEnd) {
        return "ai:weekly-report:" + subjectId + ":" + weekStart + ":" + weekEnd;
    }

    private void markDemoResponse(AiWeeklyReportResponse response) {
        if (response == null) {
            return;
        }
        response.setSource("ai-demo");
        if (StringUtils.hasText(response.getSummary())) {
            response.setSummary("[DEMO] " + response.getSummary());
        }
        if (StringUtils.hasText(response.getTrend())) {
            response.setTrend("[DEMO] " + response.getTrend());
        }
    }

    private WeekStats buildDemoWeekStats(String subjectId, LocalDate weekStart, boolean previousWeek) {
        int seed = Math.abs((subjectId + weekStart).hashCode());
        WeekStats stats = new WeekStats();
        stats.count = previousWeek ? 2 : 3;

        double observationBase = previousWeek ? 68 : 76;
        double memoryBase = previousWeek ? 62 : 74;
        double spatialBase = previousWeek ? 58 : 69;
        double calculationBase = previousWeek ? 71 : 77;
        double reasoningBase = previousWeek ? 64 : 73;
        double creativityBase = 50;

        // 临时虚拟数据，保证每次相同 subjectId 与周区间生成结果稳定，便于调试 AI 输出。
        stats.observation = round2(observationBase + (seed % 5));
        stats.memory = round2(memoryBase + ((seed / 10) % 6));
        stats.spatial = round2(spatialBase + ((seed / 100) % 7));
        stats.calculation = round2(calculationBase + ((seed / 1000) % 5));
        stats.reasoning = round2(reasoningBase + ((seed / 10000) % 6));
        stats.creativity = creativityBase;
        stats.overall = round2((stats.observation + stats.memory + stats.spatial + stats.calculation + stats.reasoning
                + stats.creativity) / 6D);
        return stats;
    }

    private Map<String, Double> buildDimensionMap(WeekStats stats) {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("观察力", stats.observation);
        map.put("记忆力", stats.memory);
        map.put("空间力", stats.spatial);
        map.put("计算力", stats.calculation);
        map.put("推理力", stats.reasoning);
        map.put("创造力", stats.creativity);
        return map;
    }

    private String buildDefaultSummary(WeekStats current) {
        if (current.count <= 0) {
            return "本周没有可用测评样本，暂时无法形成可靠分析。";
        }
        List<Map.Entry<String, Double>> sorted = buildDimensionMap(current).entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());
        String strongest = sorted.isEmpty() ? "暂无" : sorted.get(0).getKey();
        String weakest = sorted.isEmpty() ? "暂无" : sorted.get(sorted.size() - 1).getKey();
        return "本周共记录 " + current.count + " 次测评，综合分为 " + current.overall +
                "。当前最强维度是" + strongest + "，相对薄弱维度是" + weakest + "。";
    }

    private String buildDefaultTrend(WeekStats current, WeekStats previous) {
        double delta = round2(current.overall - previous.overall);
        if (current.count <= 0) {
            return "本周没有有效数据，无法进行趋势分析。";
        }
        if (previous.count <= 0) {
            return "上周缺少可比数据，本周综合分为 " + current.overall + " 分，可先以当前六维结构作为后续训练基线。";
        }
        return "相比上周，综合分" + (delta >= 0 ? "上升 " : "下降 ") + Math.abs(delta) + " 分。";
    }

    private List<String> buildDefaultStrengths(WeekStats current) {
        List<Map.Entry<String, Double>> sorted = buildDimensionMap(current).entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());
        List<String> defaults = new ArrayList<>();
        if (!sorted.isEmpty()) {
            defaults.add(sorted.get(0).getKey() + "是当前表现最稳定的维度。");
        }
        if (sorted.size() > 1) {
            defaults.add(sorted.get(1).getKey() + "维度保持在相对较高区间。");
        }
        if (defaults.isEmpty()) {
            defaults.add("当前样本不足，暂不形成稳定优势判断。");
        }
        return defaults;
    }

    private List<String> buildDefaultRisks(WeekStats current, WeekStats previous) {
        List<Map.Entry<String, Double>> sorted = buildDimensionMap(current).entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.naturalOrder()))
                .collect(Collectors.toList());
        List<String> defaults = new ArrayList<>();
        if (!sorted.isEmpty()) {
            defaults.add(sorted.get(0).getKey() + "是当前主要短板，需要优先补强。");
        }
        if (previous.count <= 0) {
            defaults.add("当前缺少上周对照数据，短期变化主要以本周六维结构为参考。");
        } else {
            defaults.add("若继续低频训练，当前薄弱维度可能继续拖累综合分。");
        }
        return defaults;
    }

    private List<String> buildDefaultSuggestions(WeekStats current) {
        List<Map.Entry<String, Double>> sorted = buildDimensionMap(current).entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.naturalOrder()))
                .collect(Collectors.toList());
        String weakest = sorted.isEmpty() ? "薄弱维度" : sorted.get(0).getKey();
        String primaryGame = mapDimensionToPrimaryGame(weakest);
        String supportGame = mapDimensionToSupportGame(weakest);
        return List.of(
                "下周训练计划以" + primaryGame + "为主，每周至少完成 3 次，每次不少于 12 分钟。",
                supportGame + "作为辅助训练，放在主训练之后完成，用来稳定其他相关维度。",
                "训练顺序固定为“主训练游戏 -> 辅助游戏”，连续执行 5 天后再观察分数变化。");
    }

    private String mapDimensionToPrimaryGame(String dimension) {
        if ("记忆力".equals(dimension) || "空间力".equals(dimension)) {
            return "记忆矩阵";
        }
        if ("计算力".equals(dimension) || "推理力".equals(dimension)) {
            return "斯特鲁普挑战";
        }
        return "舒尔特方格";
    }

    private String mapDimensionToSupportGame(String dimension) {
        if ("记忆力".equals(dimension)) {
            return "舒尔特方格";
        }
        if ("空间力".equals(dimension)) {
            return "舒尔特方格";
        }
        if ("计算力".equals(dimension) || "推理力".equals(dimension)) {
            return "记忆矩阵";
        }
        return "斯特鲁普挑战";
    }

    private double nvl(Double value) {
        return value == null ? 0D : value;
    }

    private double round2(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private static class WeekStats {
        private int count;
        private double overall;
        private double observation;
        private double memory;
        private double spatial;
        private double calculation;
        private double reasoning;
        private double creativity;
    }

    private static class AiModelOutput {
        public String summary;
        public String trend;
        public List<String> strengths;
        public List<String> risks;
        public List<String> suggestions;
    }
}
