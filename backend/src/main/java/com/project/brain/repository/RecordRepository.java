package com.project.brain.repository;

import com.project.brain.model.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordRepository extends JpaRepository<TrainingRecord, Long> {

    List<TrainingRecord> findAllBySubjectIdOrderByCreateTimeDesc(String subjectId);
}
