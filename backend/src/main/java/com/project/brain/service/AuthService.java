package com.project.brain.service;

import com.project.brain.dto.AuthLoginRequest;
import com.project.brain.dto.AuthLoginResponse;
import com.project.brain.dto.AuthProfileUpdateRequest;
import com.project.brain.dto.AuthRegisterRequest;
import com.project.brain.dto.AuthSendCodeRequest;
import com.project.brain.dto.AuthUserResponse;

public interface AuthService {

    void sendRegisterCode(AuthSendCodeRequest request);

    AuthLoginResponse register(AuthRegisterRequest request);

    AuthLoginResponse login(AuthLoginRequest request);

    AuthUserResponse me(String token);

    AuthUserResponse updateProfile(String token, AuthProfileUpdateRequest request);

    void logout(String token);
}
