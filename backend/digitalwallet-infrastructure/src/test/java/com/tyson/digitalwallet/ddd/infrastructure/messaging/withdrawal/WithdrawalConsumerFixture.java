package com.tyson.digitalwallet.ddd.infrastructure.messaging.withdrawal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.application.port.lock.DistributedLockService;
import com.tyson.digitalwallet.ddd.application.port.payment.*;
import com.tyson.digitalwallet.ddd.application.port.security.PasswordHasher;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.CreateWithdrawalCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.impl.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.pin.WalletPinAttemptService;
import com.tyson.digitalwallet.ddd.domain.model.entity.*;
import com.tyson.digitalwallet.ddd.domain.model.enums.*;
import com.tyson.digitalwallet.ddd.domain.service.ledger.LedgerService;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.*;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.*;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.repository.*;

import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.DriverManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.*;

/**
 * Real JPA + isolated PostgreSQL, deterministic provider and in-JVM replacement for existing Redis
 * lock port.
 */
class WithdrawalConsumerFixture implements AutoCloseable {
    final String url, schema = "consumer_test_" + UUID.randomUUID().toString().replace("-", "");
    final UUID walletId = UUID.randomUUID(), userId = UUID.randomUUID();
    final LocalContainerEntityManagerFactoryBean factory =
            new LocalContainerEntityManagerFactoryBean();
    final JdbcTemplate jdbc;
    final JpaTransactionManager manager;
    final TransactionTemplate tx;
    final JpaRepositoryFactory repositories;
    final WithdrawalRepositoryImpl withdrawals;
    final PayoutAttemptRepositoryImpl attempts;
    final InboxEventStoreImpl inbox;
    final CreateWithdrawalUseCase creator;
    final ProcessWithdrawalUseCase processor;
    final ConsumeWithdrawalEventUseCase consumer;
    final AtomicInteger gatewayCalls = new AtomicInteger(),
            payoutCreations = new AtomicInteger(),
            failuresLeft = new AtomicInteger();
    final AtomicReference<Runnable> beforeGateway = new AtomicReference<>(() -> {});
    final AtomicReference<PayoutGateway.Outcome> outcome =
            new AtomicReference<>(PayoutGateway.Outcome.PENDING);
    final LedgerService ledger = mock(LedgerService.class);

    WithdrawalConsumerFixture(String url) throws Exception {
        this.url = url;
        try (var c = DriverManager.getConnection(url);
                var s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        var ds = new PGSimpleDataSource();
        ds.setURL(url);
        ds.setCurrentSchema(schema);
        jdbc = new JdbcTemplate(ds);
        factory.setDataSource(ds);
        factory.setPackagesToScan(
                "com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
        factory.afterPropertiesSet();
        var emf = Objects.requireNonNull(factory.getObject());
        manager = new JpaTransactionManager(emf);
        tx = new TransactionTemplate(manager);
        var em = SharedEntityManagerCreator.createSharedEntityManager(emf);
        repositories = new JpaRepositoryFactory(em);
        repositories.addRepositoryProxyPostProcessor(
                (proxy, metadata) ->
                        proxy.addAdvice(
                                new org.springframework.dao.support
                                        .PersistenceExceptionTranslationInterceptor(factory)));
        for (var table : List.of("outbox_event", "inbox_event"))
            jdbc.execute("DROP TABLE IF EXISTS " + table);
        for (var migration : List.of("V17__create_outbox_event.sql", "V18__create_inbox_event.sql"))
            jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/" + migration)));
        tx.execute(
                s -> {
                    var user = new UserEntity();
                    user.setId(userId);
                    user.setFullName("Consumer test");
                    user.setEmail(userId + "@example.com");
                    user.setPhoneNumber("0900000000");
                    user.setPassword("test-only");
                    user.setDob(LocalDate.of(2000, 1, 1));
                    user.setStatus(UserStatus.ACTIVE);
                    em.persist(user);
                    em.persist(
                            new WalletEntity(
                                    walletId,
                                    new BigDecimal("1000000"),
                                    "VND",
                                    WalletStatus.ACTIVE,
                                    null,
                                    null,
                                    user));
                    return null;
                });
        var wallets =
                new WalletRepositoryImpl(
                        repositories.getRepository(SpringDataWalletRepository.class),
                        new WalletMapper());
        withdrawals =
                new WithdrawalRepositoryImpl(
                        repositories.getRepository(SpringDataWithdrawalRepository.class),
                        new WithdrawalMapper());
        attempts =
                new PayoutAttemptRepositoryImpl(
                        repositories.getRepository(SpringDataPayoutAttemptRepository.class),
                        new PayoutAttemptMapper());
        inbox =
                new InboxEventStoreImpl(
                        repositories.getRepository(SpringDataInboxEventRepository.class),
                        new InboxEventMapper());
        var gateway = mock(PayoutGateway.class);
        when(gateway.provider()).thenReturn("EXAMPLE");
        when(gateway.validate(any(), anyString(), anyString()))
                .thenReturn(new PayoutGateway.Quote(800, "USD", 2));
        when(gateway.execute(any(), any(), any()))
                .thenAnswer(
                        i -> {
                            gatewayCalls.incrementAndGet();
                            beforeGateway.get().run();
                            WithdrawalRequest r = i.getArgument(0);
                            PayoutAttempt a = i.getArgument(1);
                            Consumer<PayoutAttempt> checkpoint = i.getArgument(2);
                            if (tx.execute(
                                                    s ->
                                                            withdrawals
                                                                    .findByIdWithLock(r.getId())
                                                                    .orElseThrow()
                                                                    .getStatus())
                                            != WithdrawalStatus.PROCESSING
                                    && r.getStatus() != WithdrawalStatus.SUCCEEDED)
                                throw new AssertionError(
                                        "Start state must be committed before provider call");
                            if (failuresLeft.getAndUpdate(n -> Math.max(0, n - 1)) > 0)
                                throw new IllegalStateException("temporary provider timeout");
                            if (!a.getMetadata().containsKey("payout_reference")) {
                                payoutCreations.incrementAndGet();
                                checkpoint.accept(
                                        a.checkpoint("payout_reference", "fake-" + r.getId()));
                            }
                            return new PayoutGateway.Result(outcome.get(), null);
                        });
        var gateways = new PayoutGatewayRegistry(List.of(gateway));
        var locks = mock(DistributedLockService.class);
        var localLocks = new ConcurrentHashMap<String, ReentrantLock>();
        when(locks.executeWithLock(
                        anyString(),
                        anyLong(),
                        anyLong(),
                        any(),
                        org.mockito.ArgumentMatchers.<Supplier<Object>>any()))
                .thenAnswer(
                        i -> {
                            var lock =
                                    localLocks.computeIfAbsent(
                                            i.getArgument(0), k -> new ReentrantLock());
                            if (!lock.tryLock())
                                throw new com.tyson.digitalwallet.ddd.domain.exception
                                        .DistributedLockException("busy");
                            try {
                                return ((Supplier<?>) i.getArgument(4)).get();
                            } finally {
                                lock.unlock();
                            }
                        });
        var transactions =
                new TransactionRepositoryImpl(
                        repositories.getRepository(SpringDataTransactionRepository.class),
                        new TransactionMapper());
        processor =
                new ProcessWithdrawalUseCaseImpl(
                        wallets,
                        withdrawals,
                        attempts,
                        transactions,
                        ledger,
                        tx,
                        mock(ApplicationEventPublisher.class),
                        gateways,
                        locks);
        consumer = new ConsumeWithdrawalEventUseCaseImpl(inbox, withdrawals, processor, locks, tx);
        creator =
                new CreateWithdrawalUseCaseImpl(
                        wallets,
                        withdrawals,
                        attempts,
                        mock(PasswordHasher.class),
                        mock(WalletPinAttemptService.class),
                        tx,
                        gateways,
                        new OutboxEventWriterImpl(
                                repositories.getRepository(SpringDataOutboxEventRepository.class),
                                new OutboxEventMapper(new ObjectMapper())));
    }

    UUID create() {
        return creator.create(
                        new CreateWithdrawalCommand(
                                userId,
                                walletId,
                                new BigDecimal("200000"),
                                UUID.randomUUID().toString(),
                                "EXAMPLE",
                                "test-bank:SUCCESS",
                                null))
                .id();
    }

    WithdrawalStatus status(UUID id) {
        return tx.execute(s -> withdrawals.findByIdWithLock(id).orElseThrow().getStatus());
    }

    long receipts() {
        return jdbc.queryForObject("SELECT count(*) FROM inbox_event", Long.class);
    }

    @Override
    public void close() throws Exception {
        try {
            factory.destroy();
        } finally {
            try (var c = DriverManager.getConnection(url);
                    var s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }
}
