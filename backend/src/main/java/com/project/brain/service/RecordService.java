package com.project.brain.service;

import com.project.brain.model.TrainingRecord;

import java.util.List;

public interface RecordService {

    List<TrainingRecord> getHistory(String subjectId);

    void submitRecord(TrainingRecord record, String subjectId);
}
