package com.tyson.digitalwallet.ddd.infrastructure.distributed.redisson.aspect;

import com.tyson.digitalwallet.ddd.domain.annotation.DistributedLock;
import com.tyson.digitalwallet.ddd.domain.exception.DistributedLockException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class DistributedLockAspectTest {

    private RedissonClient redissonClient;
    private RLock rLock;
    private DistributedLockAspect aspect;
    private ProceedingJoinPoint joinPoint;
    private MethodSignature methodSignature;

    @Before
    public void setUp() {
        redissonClient = mock(RedissonClient.class);
        rLock = mock(RLock.class);
        aspect = new DistributedLockAspect(redissonClient);

        joinPoint = mock(ProceedingJoinPoint.class);
        methodSignature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
    }

    // Dummy sample method to reflect on
    public void dummyMethod(String senderId) {
    }

    private DistributedLock createMockAnnotation(String key, long waitTime, long leaseTime, String message) {
        DistributedLock annotation = mock(DistributedLock.class);
        when(annotation.key()).thenReturn(key);
        when(annotation.waitTime()).thenReturn(waitTime);
        when(annotation.leaseTime()).thenReturn(leaseTime);
        when(annotation.unit()).thenReturn(TimeUnit.SECONDS);
        when(annotation.message()).thenReturn(message);
        return annotation;
    }

    @Test
    public void whenLockAcquired_thenProceedAndUnlock() throws Throwable {
        Method method = this.getClass().getMethod("dummyMethod", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"wallet-123"});

        DistributedLock annotation = createMockAnnotation(
                "'lock:wallet:' + #senderId",
                0L,
                10L,
                "Transaction in progress"
        );

        when(redissonClient.getLock("lock:wallet:wallet-123")).thenReturn(rLock);
        when(rLock.tryLock(0L, 10L, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("SUCCESS");

        Object result = aspect.handleDistributedLock(joinPoint, annotation);

        assertEquals("SUCCESS", result);
        verify(joinPoint, times(1)).proceed();
        verify(rLock, times(1)).unlock();
    }

    @Test
    public void whenLockCannotBeAcquired_thenThrowDistributedLockException() throws Throwable {
        Method method = this.getClass().getMethod("dummyMethod", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"wallet-999"});

        DistributedLock annotation = createMockAnnotation(
                "'lock:wallet:' + #senderId",
                0L,
                10L,
                "Busy wallet transaction"
        );

        when(redissonClient.getLock("lock:wallet:wallet-999")).thenReturn(rLock);
        when(rLock.tryLock(0L, 10L, TimeUnit.SECONDS)).thenReturn(false);

        try {
            aspect.handleDistributedLock(joinPoint, annotation);
            fail("Expected DistributedLockException");
        } catch (DistributedLockException e) {
            assertEquals("Busy wallet transaction", e.getMessage());
        }

        verify(joinPoint, never()).proceed();
        verify(rLock, never()).unlock();
    }

    @Test
    public void whenMethodThrowsException_thenLockIsStillReleased() throws Throwable {
        Method method = this.getClass().getMethod("dummyMethod", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"wallet-err"});

        DistributedLock annotation = createMockAnnotation(
                "'lock:wallet:' + #senderId",
                0L,
                10L,
                "Busy"
        );

        when(redissonClient.getLock("lock:wallet:wallet-err")).thenReturn(rLock);
        when(rLock.tryLock(0L, 10L, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("Database failed"));

        try {
            aspect.handleDistributedLock(joinPoint, annotation);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Database failed", e.getMessage());
        }

        verify(rLock, times(1)).unlock();
    }
}
