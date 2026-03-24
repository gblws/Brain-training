package com.project.brain.controller;

import com.project.brain.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Object> handleIllegalArgument(IllegalArgumentException exception) {
        return Result.fail(400, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleOther(Exception exception) {
        return Result.fail(500, exception.getMessage());
    }
}
