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
        seedList.add(buildRecord("Schulte Grid", 96, 0.92, LocalDateTime.now().minusDays(1)));
        seedList.add(buildRecord("Stroop Challenge", 84, 0.85, LocalDateTime.now().minusDays(2)));
        seedList.add(buildRecord("Memory Matrix", 78, 0.80, LocalDateTime.now().minusDays(3)));
        seedList.add(buildRecord("Schulte Grid", 90, 0.88, LocalDateTime.now().minusDays(4)));
        recordRepository.saveAll(seedList);
    }

    @Override
    public List<TrainingRecord> getHistory() {
        return recordRepository.findAllByOrderByCreateTimeDesc();
    }

    @Override
    public void submitRecord(TrainingRecord record) {
        record.setId(null);
        // Use server local time as authoritative timestamp.
        record.setCreateTime(LocalDateTime.now());
        TrainingRecord savedRecord = recordRepository.save(record);

        try {
            System.out.println("Receive training record JSON => " + objectMapper.writeValueAsString(savedRecord));
        } catch (JsonProcessingException exception) {
            System.out.println("Receive training record parse error => " + exception.getMessage());
        }
    }

    private TrainingRecord buildRecord(String gameName, Integer score, Double accuracy, LocalDateTime createTime) {
        TrainingRecord record = new TrainingRecord();
        record.setGameName(gameName);
        record.setScore(score);
        record.setAccuracy(accuracy);
        record.setCreateTime(createTime);
        return record;
    }
}
