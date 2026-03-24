package com.project.brain.controller;

import com.project.brain.common.Result;
import com.project.brain.dto.AuthLoginRequest;
import com.project.brain.dto.AuthLoginResponse;
import com.project.brain.dto.AuthProfileUpdateRequest;
import com.project.brain.dto.AuthRegisterRequest;
import com.project.brain.dto.AuthUserResponse;
import com.project.brain.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<AuthLoginResponse> register(@RequestBody AuthRegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthLoginResponse> login(@RequestBody AuthLoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<AuthUserResponse> me(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        return Result.success(authService.me(token));
    }

    @PostMapping("/profile")
    public Result<AuthUserResponse> updateProfile(
        @RequestHeader(value = "X-Auth-Token", required = false) String token,
        @RequestBody AuthProfileUpdateRequest request
    ) {
        return Result.success(authService.updateProfile(token, request));
    }

    @PostMapping("/logout")
    public Result<Boolean> logout(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        authService.logout(token);
        return Result.success(Boolean.TRUE);
    }
}
