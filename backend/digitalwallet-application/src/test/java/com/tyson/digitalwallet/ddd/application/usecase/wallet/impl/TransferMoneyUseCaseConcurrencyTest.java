package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.TransferMoneyCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.event.MoneyTransferEvent;
import com.tyson.digitalwallet.ddd.domain.exception.DistributedLockException;
import com.tyson.digitalwallet.ddd.domain.model.entity.Transaction;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import com.tyson.digitalwallet.ddd.domain.repository.IdempotencyKeyRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.application.port.lock.DistributedLockService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.TransferResult;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Concurrency Test cho TransferMoneyUseCase.
 * Kiểm tra khả năng chống race condition, tranh chấp tài nguyên và tính toàn vẹn số dư
 * khi nhiều luồng cùng gọi transfer từ cùng một ví sender.
 */
public class TransferMoneyUseCaseConcurrencyTest {

    private WalletService walletService;
    private WalletRepository walletRepository;
    private IdempotencyKeyRepository idempotencyKeyRepository;
    private ApplicationEventPublisher eventPublisher;
    private DistributedLockService lockService;
    private TransactionTemplate transactionTemplate;
    private TransferMoneyUseCaseImpl transferMoneyUseCase;

    private final UUID senderWalletId = UUID.randomUUID();
    private final UUID senderUserId = UUID.randomUUID();
    private final UUID receiverWalletId = UUID.randomUUID();
    private final UUID receiverUserId = UUID.randomUUID();

    private Wallet senderWallet;
    private Wallet receiverWallet;

    @Before
    public void setUp() {
        walletService = mock(WalletService.class);
        walletRepository = mock(WalletRepository.class);
        idempotencyKeyRepository = mock(IdempotencyKeyRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        // Sử dụng DistributedLockService giả lập bộ nhớ dùng ReentrantLock (chuẩn ngữ nghĩa của Redis Redisson)
        lockService = new InMemoryDistributedLockService();

        // TransactionTemplate giả lập thực thi trực tiếp callback
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        transactionTemplate = new TransactionTemplate(txManager) {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(null);
            }
        };

        transferMoneyUseCase = new TransferMoneyUseCaseImpl(
                walletService,
                walletRepository,
                idempotencyKeyRepository,
                eventPublisher,
                lockService,
                transactionTemplate
        );

        senderWallet = new Wallet(
                senderWalletId,
                senderUserId,
                new BigDecimal("1000000.00"),
                "VND",
                WalletStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        receiverWallet = new Wallet(
                receiverWalletId,
                receiverUserId,
                new BigDecimal("500000.00"),
                "VND",
                WalletStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(walletRepository.findById(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findById(receiverWalletId)).thenReturn(Optional.of(receiverWallet));
    }

    @Test
    public void concurrentTransfersOnSameWallet_exactlyOneSucceedsAndOthersFailWithLockException() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger lockExceptionCount = new AtomicInteger(0);
        AtomicInteger otherExceptionCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        // Giả lập chuyển tiền: khi luồng đầu tiên giữ khóa và chạy, nó sleep 50ms để ép các luồng còn lại đụng độ lock
        when(walletService.transfer(eq(senderWalletId), eq(receiverWalletId), any(), any()))
                .thenAnswer(invocation -> {
                    Thread.sleep(60); // Giữ lock trong lúc các luồng khác ập tới
                    Transaction tx = Transaction.createTransfer(
                            senderWalletId,
                            receiverWalletId,
                            new BigDecimal("50000.00"),
                            new BigDecimal("1000000.00"),
                            new BigDecimal("950000.00"),
                            "Transfer test"
                    );
                    return new TransferResult(senderWallet, receiverWallet, tx);
                });

        for (int i = 0; i < numberOfThreads; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    barrier.await(); // Đồng bộ tất cả 10 luồng cùng xuất phát tại một mili-giây
                    TransferMoneyCommand command = new TransferMoneyCommand(
                            senderWalletId,
                            receiverWalletId,
                            new BigDecimal("50000.00"),
                            "Transfer concurrent test",
                            null // không dùng idempotency key để test thuần distributed lock
                    );
                    transferMoneyUseCase.transfer(command);
                    successCount.incrementAndGet();
                } catch (DistributedLockException e) {
                    lockExceptionCount.incrementAndGet();
                } catch (Exception e) {
                    otherExceptionCount.incrementAndGet();
                }
            }));
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (ExecutionException ignored) {}
        }
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(5, TimeUnit.SECONDS));

        // Kiểm tra kết quả:
        // Đúng 1 luồng chiếm lock thành công, 9 luồng còn lại fail-fast vì không lấy được lock
        assertEquals(1, successCount.get());
        assertEquals(9, lockExceptionCount.get());
        assertEquals(0, otherExceptionCount.get());

        // Nghiệp vụ domain chuyển tiền và gửi sự kiện chỉ được gọi đúng 1 lần duy nhất
        verify(walletService, times(1)).transfer(eq(senderWalletId), eq(receiverWalletId), any(), any());
        verify(eventPublisher, times(1)).publishEvent(any(MoneyTransferEvent.class));
    }

    @Test
    public void sequentialTransfersOnSameWallet_bothSucceedAfterLockReleased() {
        when(walletService.transfer(eq(senderWalletId), eq(receiverWalletId), any(), any()))
                .thenReturn(new TransferResult(
                        senderWallet,
                        receiverWallet,
                        Transaction.createTransfer(
                                senderWalletId,
                                receiverWalletId,
                                new BigDecimal("50000.00"),
                                new BigDecimal("1000000.00"),
                                new BigDecimal("950000.00"),
                                "Seq 1"
                        )
                ));

        TransferMoneyCommand command1 = new TransferMoneyCommand(
                senderWalletId, receiverWalletId, new BigDecimal("50000.00"), "Transfer 1", null
        );
        WalletResponse resp1 = transferMoneyUseCase.transfer(command1);
        assertNotNull(resp1);

        TransferMoneyCommand command2 = new TransferMoneyCommand(
                senderWalletId, receiverWalletId, new BigDecimal("30000.00"), "Transfer 2", null
        );
        WalletResponse resp2 = transferMoneyUseCase.transfer(command2);
        assertNotNull(resp2);

        verify(walletService, times(2)).transfer(eq(senderWalletId), eq(receiverWalletId), any(), any());
    }

    @Test
    public void concurrentTransfersOnDifferentWallets_bothSucceedConcurrently() throws Exception {
        UUID otherSenderId = UUID.randomUUID();
        Wallet otherSenderWallet = new Wallet(
                otherSenderId,
                UUID.randomUUID(),
                new BigDecimal("2000000.00"),
                "VND",
                WalletStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(walletRepository.findById(otherSenderId)).thenReturn(Optional.of(otherSenderWallet));

        when(walletService.transfer(any(), eq(receiverWalletId), any(), any()))
                .thenAnswer(invocation -> {
                    Thread.sleep(40);
                    UUID senderId = invocation.getArgument(0);
                    return new TransferResult(
                            senderId.equals(senderWalletId) ? senderWallet : otherSenderWallet,
                            receiverWallet,
                            Transaction.createTransfer(
                                    senderId,
                                    receiverWalletId,
                                    new BigDecimal("50000.00"),
                                    new BigDecimal("1000000.00"),
                                    new BigDecimal("950000.00"),
                                    "Diff wallet transfer"
                            )
                    );
                });

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger successCount = new AtomicInteger(0);

        Future<?> f1 = executorService.submit(() -> {
            try {
                barrier.await();
                transferMoneyUseCase.transfer(new TransferMoneyCommand(
                        senderWalletId, receiverWalletId, new BigDecimal("50000.00"), "Transfer from A", null
                ));
                successCount.incrementAndGet();
            } catch (Exception ignored) {}
        });

        Future<?> f2 = executorService.submit(() -> {
            try {
                barrier.await();
                transferMoneyUseCase.transfer(new TransferMoneyCommand(
                        otherSenderId, receiverWalletId, new BigDecimal("50000.00"), "Transfer from B", null
                ));
                successCount.incrementAndGet();
            } catch (Exception ignored) {}
        });

        f1.get();
        f2.get();
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(5, TimeUnit.SECONDS));

        // Cả 2 ví khác nhau đều có lock riêng nên cả 2 chuyển tiền thành công song song
        assertEquals(2, successCount.get());
        verify(walletService, times(2)).transfer(any(), eq(receiverWalletId), any(), any());
    }

    /**
     * Test Double triển khai DistributedLockService bằng java.util.concurrent.locks.ReentrantLock
     * tuân thủ đúng hợp đồng interface và logic của RedisDistributedLockServiceImpl.
     */
    private static class InMemoryDistributedLockService implements DistributedLockService {
        private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

        @Override
        public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> task) {
            ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
            boolean acquired;
            try {
                acquired = lock.tryLock(waitTime, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DistributedLockException("Thread bị gián đoạn khi chờ lock: " + key, e);
            }

            if (!acquired) {
                throw new DistributedLockException("Giao dịch đang được xử lý cho khóa: " + key);
            }

            try {
                return task.get();
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        @Override
        public void executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Runnable task) {
            executeWithLock(key, waitTime, leaseTime, unit, () -> {
                task.run();
                return null;
            });
        }
    }
}
