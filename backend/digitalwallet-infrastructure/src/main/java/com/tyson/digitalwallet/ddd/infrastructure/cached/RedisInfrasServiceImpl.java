package com.tyson.digitalwallet.ddd.infrastructure.cached;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisInfrasServiceImpl implements RedisInfrasService {

    private static final Logger log = LoggerFactory.getLogger(RedisInfrasServiceImpl.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisInfrasServiceImpl(StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    // ================= String operations =================

    @Override
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // ================= Generic Object operations (JSON) =================

    @Override
    public <T> void setObject(String key, T value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            set(key, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON for key: {}", key, e);
            throw new RuntimeException("Redis serialization error", e);
        }
    }

    @Override
    public <T> void setObject(String key, T value, long timeout, TimeUnit unit) {
        try {
            String json = objectMapper.writeValueAsString(value);
            set(key, json, timeout, unit);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON for key: {}", key, e);
            throw new RuntimeException("Redis serialization error", e);
        }
    }

    @Override
    public <T> T getObject(String key, Class<T> clazz) {
        String json = get(key);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize JSON to class {} for key: {}. Invalidating cache key.", clazz.getSimpleName(), key, e);
            delete(key);
            return null;
        }
    }

    @Override
    public <T> T getOrSet(String key, long timeout, TimeUnit unit, Class<T> clazz, java.util.function.Supplier<T> dbFallback) {
        T cached = getObject(key, clazz);
        if (cached != null) {
            return cached;
        }
        T result = dbFallback.get();
        if (result != null) {
            setObject(key, result, timeout, unit);
        }
        return result;
    }

    // ================= Key operations =================

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    @Override
    public Long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    @Override
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }
}
