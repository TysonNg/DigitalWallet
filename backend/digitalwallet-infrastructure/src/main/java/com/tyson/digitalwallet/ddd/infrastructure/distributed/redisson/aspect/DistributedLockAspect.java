package com.tyson.digitalwallet.ddd.infrastructure.distributed.redisson.aspect;

import com.tyson.digitalwallet.ddd.domain.annotation.DistributedLock;
import com.tyson.digitalwallet.ddd.domain.exception.DistributedLockException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Aspect xử lý khóa phân tán với Redisson.
 * Cấu hình Order cao (Ordered.HIGHEST_PRECEDENCE + 1) để đảm bảo:
 * Lock được lấy TRƯỚC KHI @Transactional bắt đầu, và chỉ được giải phóng SAU KHI Transaction đã commit vào DB.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class DistributedLockAspect {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockAspect.class);

    private final RedissonClient redissonClient;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public DistributedLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around("@annotation(distributedLock)")
    public Object handleDistributedLock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String key = resolveKey(distributedLock.key(), joinPoint);
        long waitTime = distributedLock.waitTime();
        long leaseTime = distributedLock.leaseTime();
        TimeUnit unit = distributedLock.unit();

        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DistributedLockException("Luồng bị gián đoạn trong khi chờ Distributed Lock: " + key, e);
        }

        if (!acquired) {
            String message = distributedLock.message();
            log.warn("Không thể chiếm Distributed Lock cho key: {} (giao dịch khác đang xử lý)", key);
            throw new DistributedLockException(message);
        }

        log.debug("Chiếm thành công Distributed Lock cho key: {}", key);
        try {
            return joinPoint.proceed();
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

    private String resolveKey(String keySpel, ProceedingJoinPoint joinPoint) {
        if (keySpel == null || keySpel.isBlank()) {
            throw new IllegalArgumentException("Biểu thức DistributedLock key không được để trống");
        }
        if (!keySpel.contains("#")) {
            return keySpel;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);

        EvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        try {
            Expression expression = expressionParser.parseExpression(keySpel);
            Object value = expression.getValue(context);
            return value != null ? value.toString() : keySpel;
        } catch (Exception e) {
            log.error("Lỗi khi phân tích biểu thức SpEL key: {}", keySpel, e);
            throw new IllegalArgumentException("Biểu thức SpEL key không hợp lệ: " + keySpel, e);
        }
    }
}
