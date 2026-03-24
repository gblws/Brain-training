package com.project.brain.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.brain.model.TrainingRecord;
import com.project.brain.repository.RecordRepository;
import com.project.brain.service.RecordService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RecordServiceImpl(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @PostConstruct
    public void initSeedData() {
        if (recordRepository.count() > 0) {
            return;
        }

        List<TrainingRecord> seedList = new ArrayList<>();
        seedList.add(buildRecord("demo_user", "Schulte Grid", 66, 0.82, 58, 2, "普通", LocalDateTime.now().minusDays(6)));
        seedList.add(buildRecord("demo_user", "Stroop Challenge", 84, 0.84, 41, 2, "普通", LocalDateTime.now().minusDays(5)));
        seedList.add(buildRecord("demo_user", "Memory Matrix", 87, 0.87, 95, 3, "困难", LocalDateTime.now().minusDays(4)));
        seedList.add(buildRecord("demo_user", "Schulte Grid", 91, 0.91, 47, 3, "困难", LocalDateTime.now().minusDays(3)));
        seedList.add(buildRecord("demo_user", "Stroop Challenge", 96, 0.96, 33, 4, "噩梦", LocalDateTime.now().minusDays(2)));
        seedList.add(buildRecord("demo_user", "Memory Matrix", 82, 0.82, 88, 2, "普通", LocalDateTime.now().minusDays(2).plusHours(3)));
        seedList.add(buildRecord("demo_user", "Schulte Grid", 98, 0.98, 39, 4, "噩梦", LocalDateTime.now().minusDays(1)));
        seedList.add(buildRecord("demo_user", "Memory Matrix", 59, 0.74, 73, 2, "普通", LocalDateTime.now().minusHours(5)));
        recordRepository.saveAll(seedList);
    }

    @Override
    public List<TrainingRecord> getHistory(String subjectId) {
        return recordRepository.findAllBySubjectIdOrderByCreateTimeDesc(subjectId);
    }

    @Override
    public void submitRecord(TrainingRecord record, String subjectId) {
        record.setId(null);
        record.setSubjectId(subjectId);
        if (record.getDurationSeconds() == null || record.getDurationSeconds() < 0) {
            record.setDurationSeconds(0);
        }
        if (record.getDifficultyLevel() == null) {
            record.setDifficultyLevel(0);
        }
        if (record.getDifficultyName() == null) {
            record.setDifficultyName("");
        }
        // Use server local time as authoritative timestamp.
        record.setCreateTime(LocalDateTime.now());
        TrainingRecord savedRecord = recordRepository.save(record);

        try {
            System.out.println("Receive training record JSON => " + objectMapper.writeValueAsString(savedRecord));
        } catch (JsonProcessingException exception) {
            System.out.println("Receive training record parse error => " + exception.getMessage());
        }
    }

    private TrainingRecord buildRecord(String subjectId, String gameName, Integer score, Double accuracy, Integer durationSeconds, Integer difficultyLevel, String difficultyName, LocalDateTime createTime) {
        TrainingRecord record = new TrainingRecord();
        record.setSubjectId(subjectId);
        record.setGameName(gameName);
        record.setScore(score);
        record.setAccuracy(accuracy);
        record.setDurationSeconds(durationSeconds);
        record.setDifficultyLevel(difficultyLevel);
        record.setDifficultyName(difficultyName);
        record.setCreateTime(createTime);
        return record;
    }
}
