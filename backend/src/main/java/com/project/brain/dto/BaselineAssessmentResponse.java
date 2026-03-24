package com.project.brain.dto;

public class BaselineAssessmentResponse {

    private String subjectId;
    private Integer stroopScore;
    private Integer schulteScore;
    private Integer memoryScore;
    private Double observationScore;
    private Double memoryScoreDimension;
    private Double spatialScore;
    private Double calculationScore;
    private Double reasoningScore;
    private Double creativityScore;
    private String matrixVersion;
    private String createTime;

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

    public Double getMemoryScoreDimension() {
        return memoryScoreDimension;
    }

    public void setMemoryScoreDimension(Double memoryScoreDimension) {
        this.memoryScoreDimension = memoryScoreDimension;
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

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
