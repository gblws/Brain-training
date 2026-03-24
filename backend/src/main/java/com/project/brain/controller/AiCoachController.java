package com.project.brain.controller;

import com.project.brain.common.Result;
import com.project.brain.dto.AiWeeklyReportResponse;
import com.project.brain.dto.AuthUserResponse;
import com.project.brain.service.AiCoachService;
import com.project.brain.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiCoachController {

    private final AiCoachService aiCoachService;
    private final AuthService authService;

    public AiCoachController(AiCoachService aiCoachService, AuthService authService) {
        this.aiCoachService = aiCoachService;
        this.authService = authService;
    }

    @GetMapping("/weekly-report")
    public Result<AiWeeklyReportResponse> weeklyReport(
        @RequestHeader(value = "X-Auth-Token", required = false) String token,
        @RequestParam(value = "refresh", required = false, defaultValue = "false") boolean refresh
    ) {
        try {
            String subjectId = resolveSubjectId(token);
            return Result.success(aiCoachService.getWeeklyReport(subjectId, refresh));
        } catch (IllegalArgumentException exception) {
            String message = exception.getMessage() == null ? "request failed" : exception.getMessage();
            if ("token is required".equals(message) || "login expired, please login again".equals(message)) {
                return Result.fail(401, message);
            }
            return Result.fail(400, message);
        }
    }

    private String resolveSubjectId(String token) {
        AuthUserResponse user = authService.me(token);
        if (user == null || user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("login expired, please login again");
        }
        return user.getUsername().trim();
    }
}
