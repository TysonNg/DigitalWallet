package com.tyson.digitalwallet.ddd.domain.service.cache;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface CacheService {

    void set(String key, String value);

    void set(String key, String value, long timeout, TimeUnit unit);

    String get(String key);

    <T> void setObject(String key, T value);

    <T> void setObject(String key, T value, long timeout, TimeUnit unit);

    <T> T getObject(String key, Class<T> clazz);

    /**
     * Tra cứu dữ liệu từ Cache theo key. Nếu có sẵn trong Cache thì trả về ngay.
     * Nếu chưa có (Cache Miss), thực thi hàm dbFallback (truy vấn DB),
     * nếu kết quả khác null thì lưu vào Cache với thời gian hết hạn timeout rồi trả về.
     */
    <T> T getOrSet(String key, long timeout, TimeUnit unit, Class<T> clazz, Supplier<T> dbFallback);

    boolean hasKey(String key);

    boolean delete(String key);

    boolean expire(String key, long timeout, TimeUnit unit);

    Long getExpire(String key, TimeUnit unit);
}
