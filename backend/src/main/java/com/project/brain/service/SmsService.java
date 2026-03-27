package com.project.brain.service;

public interface SmsService {

    void sendRegisterCode(String phoneNumber);

    void verifyRegisterCode(String phoneNumber, String verifyCode);
}
