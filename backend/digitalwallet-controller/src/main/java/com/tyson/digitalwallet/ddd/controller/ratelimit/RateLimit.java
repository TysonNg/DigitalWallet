package com.tyson.digitalwallet.ddd.controller.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for applying dynamic per-IP or per-User rate limiting via Resilience4j.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * Name of the rate limiter instance configuration in application.yaml (e.g. "authRateLimiter", "transferRateLimiter", "lookupRateLimiter").
     */
    String name();

    /**
     * Strategy used to partition rate limiting counters.
     */
    RateLimitType type() default RateLimitType.IP;
}
