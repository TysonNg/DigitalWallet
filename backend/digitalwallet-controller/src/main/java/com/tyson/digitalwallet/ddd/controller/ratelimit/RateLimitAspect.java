package com.tyson.digitalwallet.ddd.controller.ratelimit;

import com.tyson.digitalwallet.ddd.controller.security.WalletAccess;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.function.Supplier;

@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final RateLimiterRegistry rateLimiterRegistry;
    private final HttpServletRequest request;

    public RateLimitAspect(RateLimiterRegistry rateLimiterRegistry, HttpServletRequest request) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.request = request;
    }

    @Around("@annotation(rateLimit)")
    public Object applyRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = resolveKey(rateLimit.type(), joinPoint);
        String instanceName = rateLimit.name() + ":" + key;

        Supplier<RateLimiterConfig> configSupplier = () -> rateLimiterRegistry.getConfiguration(rateLimit.name())
                .orElseGet(() -> rateLimiterRegistry.find(rateLimit.name())
                        .map(RateLimiter::getRateLimiterConfig)
                        .orElseGet(rateLimiterRegistry::getDefaultConfig));

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(instanceName, configSupplier);

        boolean permission = rateLimiter.acquirePermission();
        if (!permission) {
            log.warn("Rate limit exceeded for [{}] with key [{}] on method [{}]",
                    rateLimit.name(), key, joinPoint.getSignature().toShortString());
            throw RequestNotPermitted.createRequestNotPermitted(rateLimiter);
        }

        return joinPoint.proceed();
    }

    private String resolveKey(RateLimitType type, ProceedingJoinPoint joinPoint) {
        if (type == RateLimitType.USER_OR_IP) {
            Principal principal = request.getUserPrincipal();
            if (principal == null) {
                for (Object arg : joinPoint.getArgs()) {
                    if (arg instanceof Principal p) {
                        principal = p;
                        break;
                    }
                }
            }
            if (principal != null) {
                try {
                    return "user_" + WalletAccess.currentUser(principal);
                } catch (Exception ignored) {
                    return "user_" + principal.getName();
                }
            }
        }
        return "ip_" + getClientIp();
    }

    private String getClientIp() {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null && !remoteAddr.isBlank() ? remoteAddr : "unknown";
    }
}
