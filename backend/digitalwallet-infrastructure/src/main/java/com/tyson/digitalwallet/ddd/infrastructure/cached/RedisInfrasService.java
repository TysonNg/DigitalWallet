package com.tyson.digitalwallet.ddd.infrastructure.cached;

import java.util.concurrent.TimeUnit;

public interface RedisInfrasService {

    // ================= String operations =================
    void set(String key, String value);

    void set(String key, String value, long timeout, TimeUnit unit);

    String get(String key);

    // ================= Generic Object operations (JSON) =================
    <T> void setObject(String key, T value);

    <T> void setObject(String key, T value, long timeout, TimeUnit unit);

    <T> T getObject(String key, Class<T> clazz);

    // ================= Key operations =================
    boolean hasKey(String key);

    boolean delete(String key);

    boolean expire(String key, long timeout, TimeUnit unit);

    Long getExpire(String key, TimeUnit unit);
}
