package com.project.brain.dto;

public class BaselineSubmitRequest {

    private Integer stroopScore;
    private Integer schulteScore;
    private Integer memoryScore;

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
}
