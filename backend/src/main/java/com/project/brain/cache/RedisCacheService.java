package com.project.brain.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
public class RedisCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T get(String key, Class<T> clazz) {
        try {
            String raw = stringRedisTemplate.opsForValue().get(key);
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(raw, clazz);
        } catch (Exception ignore) {
            return null;
        }
    }

    public String getString(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception ignore) {
            return null;
        }
    }

    public <T> List<T> getList(String key, Class<T> elementClass) {
        try {
            String raw = stringRedisTemplate.opsForValue().get(key);
            if (raw == null || raw.isEmpty()) {
                return Collections.emptyList();
            }
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass);
            return objectMapper.readValue(raw, listType);
        } catch (Exception ignore) {
            return Collections.emptyList();
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            String raw = objectMapper.writeValueAsString(value);
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                stringRedisTemplate.opsForValue().set(key, raw);
                return;
            }
            stringRedisTemplate.opsForValue().set(key, raw, ttl);
        } catch (JsonProcessingException ignore) {
            // ignore serialization errors to avoid breaking core flow
        } catch (Exception ignore) {
            // ignore redis errors and fallback to database
        }
    }

    public void setString(String key, String value, Duration ttl) {
        try {
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                stringRedisTemplate.opsForValue().set(key, value);
                return;
            }
            stringRedisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception ignore) {
            // ignore redis errors and fallback to database
        }
    }

    public boolean setStringIfAbsent(String key, String value, Duration ttl) {
        try {
            Boolean success;
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                success = stringRedisTemplate.opsForValue().setIfAbsent(key, value);
            } else {
                success = stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl);
            }
            return Boolean.TRUE.equals(success);
        } catch (Exception ignore) {
            return false;
        }
    }

    public void delete(String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        try {
            stringRedisTemplate.delete(List.of(keys));
        } catch (Exception ignore) {
            // ignore redis errors and fallback to database
        }
    }
}
