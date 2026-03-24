package com.project.brain.service;

import com.project.brain.dto.BaselineAssessmentResponse;
import com.project.brain.dto.BaselineSubmitRequest;

import java.util.List;

public interface BaselineAssessmentService {

    BaselineAssessmentResponse submit(BaselineSubmitRequest request, String subjectId);

    BaselineAssessmentResponse getLatest(String subjectId);

    List<BaselineAssessmentResponse> getHistory(String subjectId);
}
