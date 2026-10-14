package com.tyson.digitalwallet.ddd.infrastructure.distributed.redisson.impl;

import com.tyson.digitalwallet.ddd.infrastructure.distributed.redisson.RedisDistributedLocker;
import com.tyson.digitalwallet.ddd.infrastructure.distributed.redisson.RedisDistributedService;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class RedisDistributedServiceImpl implements RedisDistributedService {

    private final RedissonClient redissonClient;

    public RedisDistributedServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public RedisDistributedLocker getDistributedLock(String lockKey) {
        return new RedisDistributedLockerImpl(redissonClient.getLock(lockKey));
    }
}
