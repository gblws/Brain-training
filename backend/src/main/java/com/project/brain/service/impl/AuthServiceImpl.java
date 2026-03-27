package com.project.brain.service.impl;

import com.project.brain.cache.RedisCacheService;
import com.project.brain.dto.AuthLoginRequest;
import com.project.brain.dto.AuthLoginResponse;
import com.project.brain.dto.AuthProfileUpdateRequest;
import com.project.brain.dto.AuthRegisterRequest;
import com.project.brain.dto.AuthSendCodeRequest;
import com.project.brain.dto.AuthUserResponse;
import com.project.brain.model.AppUser;
import com.project.brain.repository.UserRepository;
import com.project.brain.service.AuthService;
import com.project.brain.service.SmsService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int TOKEN_DAYS = 30;
    private static final Pattern CN_PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[A-Za-z0-9]{6,18}$");
    private static final Duration PROFILE_CACHE_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final RedisCacheService redisCacheService;
    private final SmsService smsService;

    public AuthServiceImpl(UserRepository userRepository, RedisCacheService redisCacheService, SmsService smsService) {
        this.userRepository = userRepository;
        this.redisCacheService = redisCacheService;
        this.smsService = smsService;
    }

    @Override
    public void sendRegisterCode(AuthSendCodeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        String username = normalizeUsername(request.getUsername());
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("该手机号已注册");
        }
        smsService.sendRegisterCode(username);
    }

    @Override
    public AuthLoginResponse register(AuthRegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        String username = normalizeUsername(request.getUsername());
        String password = normalizePassword(request.getPassword());
        String nickname = normalizeNickname(request.getNickname(), username);
        smsService.verifyRegisterCode(username, request.getVerifyCode());

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("该手机号已注册");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(hash(password));
        user.setNickname(nickname);
        user.setAvatarUrl("");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        issueToken(user);

        AppUser saved = userRepository.save(user);
        cacheToken(saved.getLoginToken(), saved.getUsername(), saved.getTokenExpireTime());
        cacheProfile(saved);
        return toLoginResponse(saved);
    }

    @Override
    public AuthLoginResponse login(AuthLoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        String password = request.getPassword();
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("请输入密码");
        }

        AppUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("手机号或密码错误"));

        if (!hash(password).equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("手机号或密码错误");
        }

        String oldToken = user.getLoginToken();
        issueToken(user);
        user.setUpdateTime(LocalDateTime.now());
        AppUser saved = userRepository.save(user);
        evictToken(oldToken);
        cacheToken(saved.getLoginToken(), saved.getUsername(), saved.getTokenExpireTime());
        cacheProfile(saved);
        return toLoginResponse(saved);
    }

    @Override
    public AuthUserResponse me(String token) {
        String normalizedToken = normalizeToken(token);
        String username = resolveUsernameByToken(normalizedToken);

        AuthUserResponse cached = redisCacheService.get(profileKey(username), AuthUserResponse.class);
        if (cached != null) {
            return cached;
        }

        AppUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("登录已失效，请重新登录"));
        if (!normalizedToken.equals(user.getLoginToken())
            || user.getTokenExpireTime() == null
            || user.getTokenExpireTime().isBefore(LocalDateTime.now())) {
            evictToken(normalizedToken);
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }

        AuthUserResponse response = toUserResponse(user);
        redisCacheService.set(profileKey(username), response, PROFILE_CACHE_TTL);
        return response;
    }

    @Override
    public AuthUserResponse updateProfile(String token, AuthProfileUpdateRequest request) {
        AppUser user = getValidUserByToken(token);

        if (request != null) {
            if (request.getNickname() != null) {
                String nickname = request.getNickname().trim();
                if (nickname.length() > 20) {
                    throw new IllegalArgumentException("昵称长度不能超过20个字符");
                }
                user.setNickname(nickname);
            }
            if (request.getAvatarUrl() != null) {
                String avatarUrl = request.getAvatarUrl().trim();
                if (avatarUrl.length() > 512) {
                    throw new IllegalArgumentException("头像地址过长");
                }
                user.setAvatarUrl(avatarUrl);
            }
        }

        user.setUpdateTime(LocalDateTime.now());
        AppUser saved = userRepository.save(user);
        cacheProfile(saved);
        return toUserResponse(saved);
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        String normalizedToken = token.trim();
        String username = resolveUsernameFromCache(normalizedToken);
        userRepository.findByLoginToken(normalizedToken).ifPresent(user -> {
            user.setLoginToken(null);
            user.setTokenExpireTime(null);
            user.setUpdateTime(LocalDateTime.now());
            userRepository.save(user);
            evictProfile(user.getUsername());
        });
        evictToken(normalizedToken);
        if (username != null && !username.isEmpty()) {
            evictProfile(username);
        }
    }

    private AppUser getValidUserByToken(String token) {
        String normalizedToken = normalizeToken(token);
        String username = resolveUsernameByToken(normalizedToken);
        AppUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("登录已失效，请重新登录"));
        if (!normalizedToken.equals(user.getLoginToken())) {
            evictToken(normalizedToken);
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }
        if (user.getTokenExpireTime() == null || user.getTokenExpireTime().isBefore(LocalDateTime.now())) {
            evictToken(normalizedToken);
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }
        return user;
    }

    private String resolveUsernameByToken(String normalizedToken) {
        String username = resolveUsernameFromCache(normalizedToken);
        if (StringUtils.hasText(username)) {
            return username.trim();
        }

        AppUser user = userRepository.findByLoginToken(normalizedToken)
            .orElseThrow(() -> new IllegalArgumentException("登录已失效，请重新登录"));
        if (user.getTokenExpireTime() == null || user.getTokenExpireTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }
        cacheToken(normalizedToken, user.getUsername(), user.getTokenExpireTime());
        return user.getUsername();
    }

    private String resolveUsernameFromCache(String normalizedToken) {
        return redisCacheService.getString(tokenKey(normalizedToken));
    }

    private String normalizeToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("缺少登录凭证");
        }
        return token.trim();
    }

    private void issueToken(AppUser user) {
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        user.setLoginToken(token);
        user.setTokenExpireTime(LocalDateTime.now().plusDays(TOKEN_DAYS));
    }

    private void cacheToken(String token, String username, LocalDateTime expireTime) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(username) || expireTime == null) {
            return;
        }
        Duration ttl = Duration.between(LocalDateTime.now(), expireTime);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisCacheService.setString(tokenKey(token), username, ttl);
    }

    private void evictToken(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        redisCacheService.delete(tokenKey(token.trim()));
    }

    private void cacheProfile(AppUser user) {
        if (user == null || !StringUtils.hasText(user.getUsername())) {
            return;
        }
        redisCacheService.set(profileKey(user.getUsername()), toUserResponse(user), PROFILE_CACHE_TTL);
    }

    private void evictProfile(String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        redisCacheService.delete(profileKey(username.trim()));
    }

    private String tokenKey(String token) {
        return "auth:token:" + token;
    }

    private String profileKey(String username) {
        return "user:profile:" + username;
    }

    private AuthLoginResponse toLoginResponse(AppUser user) {
        AuthLoginResponse response = new AuthLoginResponse();
        response.setToken(user.getLoginToken());
        response.setUser(toUserResponse(user));
        return response;
    }

    private AuthUserResponse toUserResponse(AppUser user) {
        AuthUserResponse response = new AuthUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setCreateTime(user.getCreateTime());
        return response;
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("请输入手机号");
        }
        String value = username.trim().replace(" ", "");
        if (!CN_PHONE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("请输入有效手机号");
        }
        return value;
    }

    private String normalizeNickname(String nickname, String username) {
        if (!StringUtils.hasText(nickname)) {
            return username;
        }
        String value = nickname.trim();
        if (value.length() > 20) {
            throw new IllegalArgumentException("昵称长度不能超过20个字符");
        }
        return value;
    }

    private String normalizePassword(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("请输入密码");
        }
        String value = raw.trim();
        if (!PASSWORD_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("密码需为6-18位字母或数字");
        }
        return value;
    }

    private String hash(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("请输入密码");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("sha-256 not supported", exception);
        }
    }
}
