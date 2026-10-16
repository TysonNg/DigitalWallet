package com.tyson.digitalwallet.ddd.infrastructure.scheduler;

import com.tyson.digitalwallet.ddd.application.usecase.payment.ReconcilePendingPaymentOrdersUseCase;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class PaymentOrderReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentOrderReconciliationScheduler.class);

    private final ReconcilePendingPaymentOrdersUseCase reconcileUseCase;
    private final RedissonClient redissonClient;

    public PaymentOrderReconciliationScheduler(
            ReconcilePendingPaymentOrdersUseCase reconcileUseCase,
            RedissonClient redissonClient
    ) {
        this.reconcileUseCase = reconcileUseCase;
        this.redissonClient = redissonClient;
    }

    /**
     * Chạy định kỳ mỗi 5 phút (mặc định 300,000ms, khởi động sau 60s) để quét và đối soát các đơn nạp tiền PENDING.
     * Sử dụng Redisson Distributed Lock để đảm bảo chỉ 1 instance/pod chạy job tại 1 thời điểm.
     */
    @Scheduled(
            fixedDelayString = "${payment.reconciliation.interval-ms:300000}",
            initialDelayString = "${payment.reconciliation.initial-delay-ms:60000}"
    )
    public void runReconciliation() {
        RLock lock = redissonClient.getLock("lock:scheduler:payment-reconciliation");
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, 4, TimeUnit.MINUTES);
            if (!acquired) {
                log.debug("Another instance is running payment reconciliation job, skipping.");
                return;
            }

            log.info("Starting Payment Order reconciliation scheduled task...");
            reconcileUseCase.reconcile();
            log.info("Finished Payment Order reconciliation scheduled task.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Payment Order reconciliation scheduler was interrupted", e);
        } catch (Exception e) {
            log.error("Error occurred during payment reconciliation scheduled task", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();            }
        }
    }
}
