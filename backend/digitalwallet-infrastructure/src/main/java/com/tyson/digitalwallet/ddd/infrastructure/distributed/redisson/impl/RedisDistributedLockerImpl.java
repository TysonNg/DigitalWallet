package com.tyson.digitalwallet.ddd.infrastructure.distributed.redisson.impl;

import com.tyson.digitalwallet.ddd.infrastructure.distributed.redisson.RedisDistributedLocker;
import org.redisson.api.RLock;

import java.util.concurrent.TimeUnit;

public class RedisDistributedLockerImpl implements RedisDistributedLocker {

    private final RLock lock;

    public RedisDistributedLockerImpl(RLock lock) {
        this.lock = lock;
    }

    @Override
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        return lock.tryLock(waitTime, leaseTime, unit);
    }

    @Override
    public void lock(long leaseTime, TimeUnit unit) {
        lock.lock(leaseTime, unit);
    }

    @Override
    public void unlock() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public boolean isLocked() {
        return lock.isLocked();
    }

    @Override
    public boolean isHeldByThread(long threadId) {
        return lock.isHeldByThread(threadId);
    }

    @Override
    public boolean isHeldByCurrentThread() {
        return lock.isHeldByCurrentThread();
    }
}
