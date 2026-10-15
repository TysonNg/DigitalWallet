package com.tyson.digitalwallet.ddd.infrastructure.distributed.redisson.impl;

import com.tyson.digitalwallet.ddd.domain.exception.DistributedLockException;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RedisDistributedLockServiceImplTest {

    private RedissonClient redissonClient;
    private RLock rLock;
    private RedisDistributedLockServiceImpl lockService;

    @Before
    public void setUp() {
        redissonClient = mock(RedissonClient.class);
        rLock = mock(RLock.class);
        lockService = new RedisDistributedLockServiceImpl(redissonClient);
    }

    @Test
    public void whenLockAcquired_thenExecuteTaskAndUnlock() throws InterruptedException {
        String key = "lock:wallet:transfer:123";
        when(redissonClient.getLock(key)).thenReturn(rLock);
        when(rLock.tryLock(0L, 10L, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        String result = lockService.executeWithLock(key, () -> "EXECUTION_RESULT");

        assertEquals("EXECUTION_RESULT", result);
        verify(rLock, times(1)).unlock();
    }

    @Test
    public void whenLockNotAcquired_thenThrowDistributedLockException() throws InterruptedException {
        String key = "lock:wallet:transfer:busy";
        when(redissonClient.getLock(key)).thenReturn(rLock);
        when(rLock.tryLock(0L, 10L, TimeUnit.SECONDS)).thenReturn(false);

        AtomicBoolean taskExecuted = new AtomicBoolean(false);

        try {
            lockService.executeWithLock(key, () -> {
                taskExecuted.set(true);
                return "SHOULD_NOT_RUN";
            });
            fail("Expected DistributedLockException");
        } catch (DistributedLockException e) {
            assertTrue(e.getMessage().contains("đang được xử lý"));
        }

        assertFalse("Task must not be executed when lock fails", taskExecuted.get());
        verify(rLock, never()).unlock();
    }

    @Test
    public void whenTaskThrowsException_thenLockIsStillUnlocked() throws InterruptedException {
        String key = "lock:wallet:transfer:err";
        when(redissonClient.getLock(key)).thenReturn(rLock);
        when(rLock.tryLock(0L, 10L, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        try {
            lockService.executeWithLock(key, () -> {
                throw new IllegalStateException("Simulated business error");
            });
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Simulated business error", e.getMessage());
        }

        verify(rLock, times(1)).unlock();
    }
}
