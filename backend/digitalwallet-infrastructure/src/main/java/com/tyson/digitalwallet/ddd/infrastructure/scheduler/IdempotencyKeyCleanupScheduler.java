package com.tyson.digitalwallet.ddd.infrastructure.scheduler;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.repository.SpringDataIdempotencyKeyRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
public class IdempotencyKeyCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyCleanupScheduler.class);

    private final SpringDataIdempotencyKeyRepository repository;
    private final RedissonClient redissonClient;

    public IdempotencyKeyCleanupScheduler(
            SpringDataIdempotencyKeyRepository repository,
            RedissonClient redissonClient
    ) {
        this.repository = repository;
        this.redissonClient = redissonClient;
    }

    /**
     * Chạy định kỳ lúc 02:00 sáng mỗi ngày (giờ Việt Nam) để dọn dẹp các khóa idempotency đã hết hạn.
     * Sử dụng Redisson Distributed Lock để đảm bảo chỉ 1 instance/pod chạy job tại 1 thời điểm.
     */
    @Scheduled(cron = "${idempotency.cleanup.cron:0 0 2 * * ?}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void cleanupExpiredIdempotencyKeys() {
        RLock lock = redissonClient.getLock("lock:scheduler:idempotency-cleanup");
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, 10, TimeUnit.MINUTES);
            if (!acquired) {
                log.debug("Another instance is running idempotency key cleanup job, skipping.");
                return;
            }

            log.info("Starting expired idempotency keys cleanup task...");
            repository.deleteByExpiresAtBefore(LocalDateTime.now());
            log.info("Finished expired idempotency keys cleanup task.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Idempotency key cleanup scheduler was interrupted", e);
        } catch (Exception e) {
            log.error("Error occurred during idempotency key cleanup task", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
