package com.tyson.digitalwallet.ddd.infrastructure.distributed.redisson.impl;

import com.tyson.digitalwallet.ddd.domain.exception.DistributedLockException;
import com.tyson.digitalwallet.ddd.application.port.lock.DistributedLockService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class RedisDistributedLockServiceImpl implements DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedLockServiceImpl.class);

    private final RedissonClient redissonClient;

    public RedisDistributedLockServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> task) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Lock key must not be blank");
        }

        RLock lock = redissonClient.getLock(key);
        boolean acquired;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DistributedLockException("Luồng bị gián đoạn trong khi chờ Distributed Lock: " + key, e);
        }

        if (!acquired) {
            log.warn("Không thể chiếm Distributed Lock cho key: {} (đang có giao dịch khác xử lý)", key);
            throw new DistributedLockException("Giao dịch trước đó của bạn đang được xử lý, vui lòng chờ trong giây lát");
        }

        log.debug("Chiếm thành công Distributed Lock cho key: {}", key);
        try {
            return task.get();
        } finally {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("Đã giải phóng Distributed Lock cho key: {}", key);
                }
            } catch (Exception e) {
                log.error("Lỗi khi giải phóng Distributed Lock cho key: {}", key, e);
            }
        }
    }

    @Override
    public void executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Runnable task) {
        executeWithLock(key, waitTime, leaseTime, unit, () -> {
            task.run();
            return null;
        });
    }
}
