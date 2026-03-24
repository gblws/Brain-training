package com.project.brain.controller;

import com.project.brain.common.Result;
import com.project.brain.dto.AuthUserResponse;
import com.project.brain.dto.BaselineAssessmentResponse;
import com.project.brain.dto.BaselineSubmitRequest;
import com.project.brain.service.AuthService;
import com.project.brain.service.BaselineAssessmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/baseline")
public class BaselineAssessmentController {

    private final BaselineAssessmentService baselineAssessmentService;
    private final AuthService authService;

    public BaselineAssessmentController(BaselineAssessmentService baselineAssessmentService, AuthService authService) {
        this.baselineAssessmentService = baselineAssessmentService;
        this.authService = authService;
    }

    @PostMapping("/submit")
    public Result<BaselineAssessmentResponse> submit(
        @RequestHeader(value = "X-Auth-Token", required = false) String token,
        @RequestBody BaselineSubmitRequest request
    ) {
        try {
            String subjectId = resolveSubjectId(token);
            return Result.success("baseline submit success", baselineAssessmentService.submit(request, subjectId));
        } catch (IllegalArgumentException exception) {
            return toFailure(exception);
        }
    }

    @GetMapping("/latest")
    public Result<BaselineAssessmentResponse> latest(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        try {
            String subjectId = resolveSubjectId(token);
            BaselineAssessmentResponse response = baselineAssessmentService.getLatest(subjectId);
            if (response == null) {
                return Result.success(null);
            }
            return Result.success(response);
        } catch (IllegalArgumentException exception) {
            return toFailure(exception);
        }
    }

    @GetMapping("/history")
    public Result<List<BaselineAssessmentResponse>> history(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        try {
            String subjectId = resolveSubjectId(token);
            return Result.success(baselineAssessmentService.getHistory(subjectId));
        } catch (IllegalArgumentException exception) {
            return toFailure(exception);
        }
    }

    private String resolveSubjectId(String token) {
        AuthUserResponse user = authService.me(token);
        if (user == null || user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("login expired, please login again");
        }
        return user.getUsername().trim();
    }

    private <T> Result<T> toFailure(IllegalArgumentException exception) {
        String message = exception.getMessage() == null ? "request failed" : exception.getMessage();
        if ("token is required".equals(message) || "login expired, please login again".equals(message)) {
            return Result.fail(401, message);
        }
        return Result.fail(400, message);
    }
}
