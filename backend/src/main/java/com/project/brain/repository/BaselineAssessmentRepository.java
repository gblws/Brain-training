package com.project.brain.repository;

import com.project.brain.model.BaselineAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BaselineAssessmentRepository extends JpaRepository<BaselineAssessment, Long> {

    Optional<BaselineAssessment> findTopBySubjectIdOrderByCreateTimeDesc(String subjectId);

    List<BaselineAssessment> findAllBySubjectIdOrderByCreateTimeDesc(String subjectId);

    Optional<BaselineAssessment> findTopBySubjectIdAndCreateTimeBetweenOrderByCreateTimeDesc(
        String subjectId,
        LocalDateTime start,
        LocalDateTime end
    );
}
