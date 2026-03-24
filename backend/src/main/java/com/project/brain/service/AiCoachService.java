package com.project.brain.service;

import com.project.brain.dto.AiWeeklyReportResponse;

public interface AiCoachService {

    AiWeeklyReportResponse getWeeklyReport(String subjectId, boolean refresh);
}
