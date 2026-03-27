package com.project.brain.service.impl;

import com.project.brain.model.TrainingRecord;
import com.project.brain.repository.RecordRepository;
import com.project.brain.service.RecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;

    public RecordServiceImpl(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
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
        recordRepository.save(record);
    }
}
