package com.project.brain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "baseline_assessment")
// 基准线评估实体类
public class BaselineAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_id", nullable = false, length = 64)
    private String subjectId;

    @Column(name = "stroop_score", nullable = false)
    private Integer stroopScore;

    @Column(name = "schulte_score", nullable = false)
    private Integer schulteScore;

    @Column(name = "memory_score", nullable = false)
    private Integer memoryScore;

    @Column(name = "observation_score", nullable = false)
    private Double observationScore;

    @Column(name = "memory_dimension_score", nullable = false)
    private Double memoryDimensionScore;

    @Column(name = "spatial_score", nullable = false)
    private Double spatialScore;

    @Column(name = "calculation_score", nullable = false)
    private Double calculationScore;

    @Column(name = "reasoning_score", nullable = false)
    private Double reasoningScore;

    @Column(name = "creativity_score", nullable = false)
    private Double creativityScore;

    @Column(name = "matrix_version", nullable = false, length = 16)
    private String matrixVersion;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public Integer getStroopScore() {
        return stroopScore;
    }

    public void setStroopScore(Integer stroopScore) {
        this.stroopScore = stroopScore;
    }

    public Integer getSchulteScore() {
        return schulteScore;
    }

    public void setSchulteScore(Integer schulteScore) {
        this.schulteScore = schulteScore;
    }

    public Integer getMemoryScore() {
        return memoryScore;
    }

    public void setMemoryScore(Integer memoryScore) {
        this.memoryScore = memoryScore;
    }

    public Double getObservationScore() {
        return observationScore;
    }

    public void setObservationScore(Double observationScore) {
        this.observationScore = observationScore;
    }

    public Double getMemoryDimensionScore() {
        return memoryDimensionScore;
    }

    public void setMemoryDimensionScore(Double memoryDimensionScore) {
        this.memoryDimensionScore = memoryDimensionScore;
    }

    public Double getSpatialScore() {
        return spatialScore;
    }

    public void setSpatialScore(Double spatialScore) {
        this.spatialScore = spatialScore;
    }

    public Double getCalculationScore() {
        return calculationScore;
    }

    public void setCalculationScore(Double calculationScore) {
        this.calculationScore = calculationScore;
    }

    public Double getReasoningScore() {
        return reasoningScore;
    }

    public void setReasoningScore(Double reasoningScore) {
        this.reasoningScore = reasoningScore;
    }

    public Double getCreativityScore() {
        return creativityScore;
    }

    public void setCreativityScore(Double creativityScore) {
        this.creativityScore = creativityScore;
    }

    public String getMatrixVersion() {
        return matrixVersion;
    }

    public void setMatrixVersion(String matrixVersion) {
        this.matrixVersion = matrixVersion;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
