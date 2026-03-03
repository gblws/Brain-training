package com.project.brain.controller;

import com.project.brain.common.Result;
import com.project.brain.model.TrainingRecord;
import com.project.brain.service.RecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/user/history")
    public Result<List<TrainingRecord>> getUserHistory() {
        return Result.success(recordService.getHistory());
    }

    @PostMapping("/game/submit")
    public Result<Boolean> submitRecord(@RequestBody TrainingRecord record) {
        recordService.submitRecord(record);
        return Result.success("submit success", Boolean.TRUE);
    }
}
