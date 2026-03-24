package com.project.brain.service.impl;

import com.project.brain.cache.RedisCacheService;
import com.project.brain.dto.BaselineAssessmentResponse;
import com.project.brain.dto.BaselineSubmitRequest;
import com.project.brain.model.BaselineAssessment;
import com.project.brain.repository.BaselineAssessmentRepository;
import com.project.brain.service.BaselineAssessmentService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BaselineAssessmentServiceImpl implements BaselineAssessmentService {

    private static final String MATRIX_VERSION = "v1";
    private static final double CREATIVITY_BASELINE = 50D;
    private static final Duration BASELINE_LATEST_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration BASELINE_HISTORY_CACHE_TTL = Duration.ofMinutes(3);

    private final BaselineAssessmentRepository baselineAssessmentRepository;
    private final RedisCacheService redisCacheService;

    public BaselineAssessmentServiceImpl(
        BaselineAssessmentRepository baselineAssessmentRepository,
        RedisCacheService redisCacheService
    ) {
        this.baselineAssessmentRepository = baselineAssessmentRepository;
        this.redisCacheService = redisCacheService;
    }

    @Override
    public BaselineAssessmentResponse submit(BaselineSubmitRequest request, String subjectId) {
        validateRequest(request, subjectId);
        String normalizedSubjectId = subjectId.trim();

        double stroop = clampScore(request.getStroopScore());
        double schulte = clampScore(request.getSchulteScore());
        double memory = clampScore(request.getMemoryScore());

        double observationScore = round2(weightedAverage(stroop, 0.40, schulte, 0.75, memory, 0.05));
        double memoryDimensionScore = round2(weightedAverage(stroop, 0.00, schulte, 0.10, memory, 0.65));
        double spatialScore = round2(weightedAverage(stroop, 0.00, schulte, 0.10, memory, 0.30));
        double calculationScore = round2(weightedAverage(stroop, 0.35, schulte, 0.05, memory, 0.00));
        double reasoningScore = round2(weightedAverage(stroop, 0.25, schulte, 0.00, memory, 0.00));

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);
        BaselineAssessment assessment = baselineAssessmentRepository
            .findTopBySubjectIdAndCreateTimeBetweenOrderByCreateTimeDesc(normalizedSubjectId, start, end)
            .orElseGet(BaselineAssessment::new);

        assessment.setSubjectId(normalizedSubjectId);
        assessment.setStroopScore((int) stroop);
        assessment.setSchulteScore((int) schulte);
        assessment.setMemoryScore((int) memory);
        assessment.setObservationScore(observationScore);
        assessment.setMemoryDimensionScore(memoryDimensionScore);
        assessment.setSpatialScore(spatialScore);
        assessment.setCalculationScore(calculationScore);
        assessment.setReasoningScore(reasoningScore);
        assessment.setCreativityScore(CREATIVITY_BASELINE);
        assessment.setMatrixVersion(MATRIX_VERSION);
        assessment.setCreateTime(LocalDateTime.now());

        BaselineAssessment saved = baselineAssessmentRepository.save(assessment);
        BaselineAssessmentResponse response = toResponse(saved);
        evictBaselineCache(normalizedSubjectId);
        redisCacheService.set(latestKey(normalizedSubjectId), response, BASELINE_LATEST_CACHE_TTL);
        return response;
    }

    @Override
    public BaselineAssessmentResponse getLatest(String subjectId) {
        if (subjectId == null || subjectId.trim().isEmpty()) {
            return null;
        }

        String normalizedSubjectId = subjectId.trim();
        BaselineAssessmentResponse cached = redisCacheService.get(latestKey(normalizedSubjectId), BaselineAssessmentResponse.class);
        if (cached != null) {
            return cached;
        }

        BaselineAssessmentResponse latest = baselineAssessmentRepository
            .findTopBySubjectIdOrderByCreateTimeDesc(normalizedSubjectId)
            .map(this::toResponse)
            .orElse(null);
        if (latest != null) {
            redisCacheService.set(latestKey(normalizedSubjectId), latest, BASELINE_LATEST_CACHE_TTL);
        }
        return latest;
    }

    @Override
    public List<BaselineAssessmentResponse> getHistory(String subjectId) {
        if (subjectId == null || subjectId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedSubjectId = subjectId.trim();
        List<BaselineAssessmentResponse> cached = redisCacheService.getList(historyKey(normalizedSubjectId), BaselineAssessmentResponse.class);
        if (!cached.isEmpty()) {
            return cached;
        }

        List<BaselineAssessmentResponse> history = baselineAssessmentRepository
            .findAllBySubjectIdOrderByCreateTimeDesc(normalizedSubjectId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        if (!history.isEmpty()) {
            redisCacheService.set(historyKey(normalizedSubjectId), history, BASELINE_HISTORY_CACHE_TTL);
        }
        return history;
    }

    private void validateRequest(BaselineSubmitRequest request, String subjectId) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (subjectId == null || subjectId.trim().isEmpty()) {
            throw new IllegalArgumentException("subjectId is required");
        }
        if (request.getStroopScore() == null || request.getSchulteScore() == null || request.getMemoryScore() == null) {
            throw new IllegalArgumentException("stroopScore/schulteScore/memoryScore are required");
        }
    }

    private double clampScore(Integer score) {
        if (score == null) {
            return 0D;
        }
        return Math.max(0D, Math.min(100D, score));
    }

    private double weightedAverage(
        double s1, double w1,
        double s2, double w2,
        double s3, double w3
    ) {
        double weightSum = w1 + w2 + w3;
        if (weightSum <= 0D) {
            return 0D;
        }
        return (s1 * w1 + s2 * w2 + s3 * w3) / weightSum;
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private BaselineAssessmentResponse toResponse(BaselineAssessment assessment) {
        BaselineAssessmentResponse response = new BaselineAssessmentResponse();
        response.setSubjectId(assessment.getSubjectId());
        response.setStroopScore(assessment.getStroopScore());
        response.setSchulteScore(assessment.getSchulteScore());
        response.setMemoryScore(assessment.getMemoryScore());
        response.setObservationScore(assessment.getObservationScore());
        response.setMemoryScoreDimension(assessment.getMemoryDimensionScore());
        response.setSpatialScore(assessment.getSpatialScore());
        response.setCalculationScore(assessment.getCalculationScore());
        response.setReasoningScore(assessment.getReasoningScore());
        response.setCreativityScore(assessment.getCreativityScore());
        response.setMatrixVersion(assessment.getMatrixVersion());
        response.setCreateTime(String.valueOf(assessment.getCreateTime()));
        return response;
    }

    private void evictBaselineCache(String subjectId) {
        if (!StringUtils.hasText(subjectId)) {
            return;
        }
        redisCacheService.delete(latestKey(subjectId.trim()), historyKey(subjectId.trim()));
    }

    private String latestKey(String subjectId) {
        return "baseline:latest:" + subjectId;
    }

    private String historyKey(String subjectId) {
        return "baseline:history:" + subjectId;
    }
}
