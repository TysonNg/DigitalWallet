package com.tyson.digitalwallet.ddd.controller.ratelimit;

import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.enums.ErrorStatus;
import com.tyson.digitalwallet.ddd.controller.exception.GlobalExceptionHandler;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.annotation.Annotation;
import java.security.Principal;
import java.time.Duration;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RateLimitAspectTest {

    private RateLimiterRegistry registry;
    private MockHttpServletRequest request;
    private RateLimitAspect aspect;
    private GlobalExceptionHandler exceptionHandler;

    @Before
    public void setup() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(2)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build();
        registry = RateLimiterRegistry.of(config);
        request = new MockHttpServletRequest();
        aspect = new RateLimitAspect(registry, request);
        exceptionHandler = new GlobalExceptionHandler();
    }

    private RateLimit createRateLimit(String name, RateLimitType type) {
        return new RateLimit() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return RateLimit.class;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public RateLimitType type() {
                return type;
            }
        };
    }

    private ProceedingJoinPoint mockJoinPoint(Object... args) throws Throwable {
        ProceedingJoinPoint jp = mock(ProceedingJoinPoint.class);
        Signature sig = mock(Signature.class);
        when(sig.toShortString()).thenReturn("testMethod()");
        when(jp.getSignature()).thenReturn(sig);
        when(jp.getArgs()).thenReturn(args);
        when(jp.proceed()).thenReturn("SUCCESS");
        return jp;
    }

    @Test
    public void testPerIpRateLimiting_AllowsUpToLimitThenBlocksSameIp() throws Throwable {
        request.setRemoteAddr("192.168.1.100");
        RateLimit annotation = createRateLimit("testAuthLimit", RateLimitType.IP);
        ProceedingJoinPoint jp = mockJoinPoint();

        // First 2 calls should succeed
        assertEquals("SUCCESS", aspect.applyRateLimit(jp, annotation));
        assertEquals("SUCCESS", aspect.applyRateLimit(jp, annotation));

        // 3rd call from same IP must be blocked
        try {
            aspect.applyRateLimit(jp, annotation);
            fail("Expected RequestNotPermitted exception");
        } catch (RequestNotPermitted ex) {
            // Verify exception handling converts to HTTP 429
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleRateLimit(ex);
            assertEquals(429, response.getStatusCode().value());
            assertEquals(ErrorStatus.TOO_MANY_REQUESTS.getCode(), response.getBody().statusCode());
            assertEquals("TOO_MANY_REQUESTS", response.getBody().errorCode());
        }
    }

    @Test
    public void testDifferentIpsHaveSeparateQuotas() throws Throwable {
        RateLimit annotation = createRateLimit("testAuthLimit", RateLimitType.IP);
        ProceedingJoinPoint jp = mockJoinPoint();

        // IP 1 uses up its 2 permits
        request.setRemoteAddr("10.0.0.1");
        assertEquals("SUCCESS", aspect.applyRateLimit(jp, annotation));
        assertEquals("SUCCESS", aspect.applyRateLimit(jp, annotation));

        // IP 2 must still be allowed because each IP has an independent quota!
        request.setRemoteAddr("10.0.0.2");
        assertEquals("SUCCESS", aspect.applyRateLimit(jp, annotation));
        assertEquals("SUCCESS", aspect.applyRateLimit(jp, annotation));
    }

    @Test
    public void testPerUserRateLimiting_DifferentUsersHaveSeparateQuotas() throws Throwable {
        RateLimit annotation = createRateLimit("testTransferLimit", RateLimitType.USER_OR_IP);

        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        Principal principalA = userA::toString;
        Principal principalB = userB::toString;

        ProceedingJoinPoint jpA = mockJoinPoint(principalA);
        ProceedingJoinPoint jpB = mockJoinPoint(principalB);

        // User A uses up their quota
        assertEquals("SUCCESS", aspect.applyRateLimit(jpA, annotation));
        assertEquals("SUCCESS", aspect.applyRateLimit(jpA, annotation));

        // User A is blocked on 3rd call
        try {
            aspect.applyRateLimit(jpA, annotation);
            fail("Expected User A to be blocked");
        } catch (RequestNotPermitted ignored) {
        }

        // User B is NOT blocked
        assertEquals("SUCCESS", aspect.applyRateLimit(jpB, annotation));
    }
}
