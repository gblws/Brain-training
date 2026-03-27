package com.project.brain.service.impl;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teaopenapi.models.Config;
import com.project.brain.cache.RedisCacheService;
import com.project.brain.service.SmsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class SmsServiceImpl implements SmsService {

    private static final Pattern CN_PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern VERIFY_CODE_PATTERN = Pattern.compile("^\\d{6}$");

    private final RedisCacheService redisCacheService;

    @Value("${sms.aliyun.access-key-id:}")
    private String accessKeyId;

    @Value("${sms.aliyun.access-key-secret:}")
    private String accessKeySecret;

    @Value("${sms.aliyun.endpoint:dypnsapi.aliyuncs.com}")
    private String endpoint;

    @Value("${sms.aliyun.region-id:cn-beijing}")
    private String regionId;

    @Value("${sms.aliyun.country-code:86}")
    private String countryCode;

    @Value("${sms.aliyun.sign-name:}")
    private String signName;

    @Value("${sms.aliyun.template-code:}")
    private String templateCode;

    @Value("${sms.aliyun.code-length:6}")
    private Integer codeLength;

    @Value("${sms.aliyun.valid-minutes:5}")
    private Integer validMinutes;

    @Value("${sms.aliyun.interval-seconds:60}")
    private Integer intervalSeconds;

    public SmsServiceImpl(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    @Override
    public void sendRegisterCode(String phoneNumber) {
        String normalizedPhone = normalizePhone(phoneNumber);
        validateConfig();

        Duration interval = Duration.ofSeconds(Math.max(10, intervalSeconds == null ? 60 : intervalSeconds));
        boolean allowed = redisCacheService.setStringIfAbsent(sendLimitKey(normalizedPhone), "1", interval);
        if (!allowed) {
            throw new IllegalArgumentException("发送过于频繁，请稍后再试");
        }

        String verifyCode = generateVerifyCode();
        try {
            Client client = createClient();
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(normalizedPhone)
                    .setCountryCode(countryCode)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam(buildTemplateParam(verifyCode))
                    .setCodeLength(Long.valueOf(Math.max(4, codeLength == null ? 6 : codeLength)))
                    .setValidTime(Long.valueOf(Math.max(1, validMinutes == null ? 5 : validMinutes)))
                    .setInterval(Long.valueOf(Math.max(10, intervalSeconds == null ? 60 : intervalSeconds)))
                    .setCodeType(1L);
            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCode(request);
            String responseCode = response == null || response.getBody() == null ? null : response.getBody().getCode();
            if (!"OK".equalsIgnoreCase(responseCode)) {
                String message = response != null && response.getBody() != null && StringUtils.hasText(response.getBody().getMessage())
                        ? response.getBody().getMessage()
                        : "验证码发送失败";
                redisCacheService.delete(sendLimitKey(normalizedPhone));
                throw new IllegalArgumentException(message);
            }
            redisCacheService.setString(codeKey(normalizedPhone), verifyCode, Duration.ofMinutes(Math.max(1, validMinutes == null ? 5 : validMinutes)));
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            redisCacheService.delete(sendLimitKey(normalizedPhone));
            throw new IllegalArgumentException("验证码发送失败，请稍后重试");
        }
    }

    @Override
    public void verifyRegisterCode(String phoneNumber, String verifyCode) {
        String normalizedPhone = normalizePhone(phoneNumber);
        if (!StringUtils.hasText(verifyCode)) {
            throw new IllegalArgumentException("请输入验证码");
        }
        String normalizedCode = verifyCode.trim();
        if (!VERIFY_CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new IllegalArgumentException("验证码格式不正确");
        }
        String cachedCode = redisCacheService.getString(codeKey(normalizedPhone));
        if (!StringUtils.hasText(cachedCode)) {
            throw new IllegalArgumentException("验证码已失效，请重新获取");
        }
        if (!normalizedCode.equals(cachedCode.trim())) {
            throw new IllegalArgumentException("验证码错误");
        }
        redisCacheService.delete(codeKey(normalizedPhone), sendLimitKey(normalizedPhone));
    }

    private Client createClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint(endpoint)
                .setRegionId(regionId);
        return new Client(config);
    }

    private void validateConfig() {
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret)) {
            throw new IllegalArgumentException("短信服务密钥未配置");
        }
        if (!StringUtils.hasText(signName) || !StringUtils.hasText(templateCode)) {
            throw new IllegalArgumentException("短信模板配置不完整");
        }
    }

    private String normalizePhone(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            throw new IllegalArgumentException("请输入手机号");
        }
        String normalized = phoneNumber.trim().replace(" ", "");
        if (!CN_PHONE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("请输入有效手机号");
        }
        return normalized;
    }

    private String generateVerifyCode() {
        int length = Math.max(4, codeLength == null ? 6 : codeLength);
        int bound = (int) Math.pow(10, length);
        int start = (int) Math.pow(10, length - 1);
        return String.valueOf(ThreadLocalRandom.current().nextInt(start, bound));
    }

    private String buildTemplateParam(String verifyCode) {
        return String.format("{\"code\":\"%s\",\"min\":\"%s\"}", verifyCode, Math.max(1, validMinutes == null ? 5 : validMinutes));
    }

    private String codeKey(String phoneNumber) {
        return "auth:sms:code:" + phoneNumber;
    }

    private String sendLimitKey(String phoneNumber) {
        return "auth:sms:limit:" + phoneNumber;
    }
}
