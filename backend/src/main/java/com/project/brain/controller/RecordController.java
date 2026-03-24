package com.project.brain.controller;

import com.project.brain.common.Result;
import com.project.brain.dto.AuthUserResponse;
import com.project.brain.model.TrainingRecord;
import com.project.brain.service.AuthService;
import com.project.brain.service.RecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class RecordController {

    private final RecordService recordService;
    private final AuthService authService;

    public RecordController(RecordService recordService, AuthService authService) {
        this.recordService = recordService;
        this.authService = authService;
    }

    @GetMapping("/user/history")
    public Result<List<TrainingRecord>> getUserHistory(
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        try {
            return Result.success(recordService.getHistory(resolveSubjectId(token)));
        } catch (IllegalArgumentException exception) {
            return toFailure(exception);
        }
    }

    @PostMapping("/game/submit")
    public Result<Boolean> submitRecord(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestBody TrainingRecord record) {
        try {
            recordService.submitRecord(record, resolveSubjectId(token));
            return Result.success("submit success", Boolean.TRUE);
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
