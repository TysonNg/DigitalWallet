package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.application.port.outbox.*;
import com.tyson.digitalwallet.ddd.application.port.payment.*;
import com.tyson.digitalwallet.ddd.application.port.security.PasswordHasher;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.CreateWithdrawalCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.impl.CreateWithdrawalUseCaseImpl;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.pin.WalletPinAttemptService;
import com.tyson.digitalwallet.ddd.domain.model.enums.*;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.*;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.*;

import jakarta.persistence.*;

import org.junit.*;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Run only against an isolated database with -Doutbox.test.jdbc=jdbc:postgresql://... . */
public class OutboxAtomicityPostgresTest {
    private String url, schema;
    private final UUID walletId = UUID.randomUUID(), userId = UUID.randomUUID();
    private LocalContainerEntityManagerFactoryBean factory;
    private EntityManager em;
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private OutboxEventWriter writer;

    @Before
    public void setup() throws Exception {
        url = System.getProperty("outbox.test.jdbc");
        Assume.assumeTrue("Isolated PostgreSQL URL required", url != null);
        schema = "outbox_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var c = DriverManager.getConnection(url);
                var s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        var ds = new PGSimpleDataSource();
        ds.setURL(url);
        ds.setCurrentSchema(schema);
        jdbc = new JdbcTemplate(ds);
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(ds);
        factory.setPackagesToScan(
                "com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
        factory.afterPropertiesSet();
        var emf = Objects.requireNonNull(factory.getObject());
        tx = new TransactionTemplate(new JpaTransactionManager(emf));
        em = SharedEntityManagerCreator.createSharedEntityManager(emf);
        jdbc.execute("DROP TABLE IF EXISTS outbox_event");
        jdbc.execute(
                Files.readString(
                        Path.of("src/main/resources/db/migration/V17__create_outbox_event.sql")));
        var repositoryFactory = new JpaRepositoryFactory(em);
        repositoryFactory.addRepositoryProxyPostProcessor(
                (proxy, metadata) ->
                        proxy.addAdvice(
                                new org.springframework.dao.support
                                        .PersistenceExceptionTranslationInterceptor(factory)));
        var target =
                new OutboxEventWriterImpl(
                        repositoryFactory.getRepository(SpringDataOutboxEventRepository.class),
                        new OutboxEventMapper(new ObjectMapper()));
        var proxy = new org.springframework.aop.framework.ProxyFactory(target);
        proxy.addAdvice(
                new org.springframework.transaction.interceptor.TransactionInterceptor(
                        tx.getTransactionManager(),
                        new org.springframework.transaction.annotation
                                .AnnotationTransactionAttributeSource()));
        writer = (OutboxEventWriter) proxy.getProxy();
        tx.execute(
                s -> {
                    var user = new UserEntity();
                    user.setId(userId);
                    user.setFullName("Outbox test");
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
    }

    @After
    public void cleanup() throws Exception {
        try {
            if (factory != null && factory.getObject() != null) factory.destroy();
        } finally {
            if (schema != null)
                try (var c = DriverManager.getConnection(url);
                        var s = c.createStatement()) {
                    s.execute("DROP SCHEMA " + schema + " CASCADE");
                }
        }
    }

    private CreateWithdrawalUseCaseImpl service() {
        var repositories = new JpaRepositoryFactory(em);
        var wallets =
                new WalletRepositoryImpl(
                        repositories.getRepository(SpringDataWalletRepository.class),
                        new WalletMapper());
        var requests =
                new WithdrawalRepositoryImpl(
                        repositories.getRepository(SpringDataWithdrawalRepository.class),
                        new WithdrawalMapper());
        var attempts =
                new PayoutAttemptRepositoryImpl(
                        repositories.getRepository(SpringDataPayoutAttemptRepository.class),
                        new PayoutAttemptMapper());
        var gateway = mock(PayoutGateway.class);
        when(gateway.provider()).thenReturn("EXAMPLE");
        when(gateway.validate(any(), anyString(), anyString()))
                .thenReturn(new PayoutGateway.Quote(800, "USD", 2));
        return new CreateWithdrawalUseCaseImpl(
                wallets,
                requests,
                attempts,
                mock(PasswordHasher.class),
                mock(WalletPinAttemptService.class),
                tx,
                new PayoutGatewayRegistry(List.of(gateway)),
                writer);
    }

    private CreateWithdrawalCommand command() {
        return new CreateWithdrawalCommand(
                userId,
                walletId,
                new BigDecimal("200000"),
                "outbox-request-key",
                "EXAMPLE",
                "bank-token",
                null);
    }

    private void assertRolledBack() {
        assertEquals(
                0,
                jdbc.queryForObject("SELECT count(*) FROM withdrawal_requests", Integer.class)
                        .intValue());
        assertEquals(
                0,
                jdbc.queryForObject("SELECT count(*) FROM payout_attempts", Integer.class)
                        .intValue());
        assertEquals(
                0,
                jdbc.queryForObject("SELECT count(*) FROM outbox_event", Integer.class).intValue());
        assertEquals(
                0,
                new BigDecimal("1000000")
                        .compareTo(
                                jdbc.queryForObject(
                                        "SELECT balance FROM wallets WHERE id=?",
                                        BigDecimal.class,
                                        walletId)));
    }

    @Test
    public void commitsBusinessChangesAndOutboxTogetherAndReplayAppendsNothing() {
        var service = service();
        var r = service.create(command());
        assertEquals(r.id(), service.create(command()).id());
        assertEquals(
                1,
                jdbc.queryForObject("SELECT count(*) FROM outbox_event", Integer.class).intValue());
        assertEquals(
                1,
                jdbc.queryForObject("SELECT count(*) FROM payout_attempts", Integer.class)
                        .intValue());
        assertEquals(
                0,
                new BigDecimal("800000")
                        .compareTo(
                                jdbc.queryForObject(
                                        "SELECT balance FROM wallets WHERE id=?",
                                        BigDecimal.class,
                                        walletId)));
        var event =
                jdbc.queryForMap(
                        "SELECT"
                            + " aggregate_type,aggregate_id,event_type,schema_version,destination,payload->>'withdrawalId'"
                            + " AS withdrawal_id,published_at,attempts FROM outbox_event");
        assertEquals("WITHDRAWAL", event.get("aggregate_type"));
        assertEquals(r.id().toString(), event.get("aggregate_id"));
        assertEquals("WITHDRAWAL_PROCESS_REQUESTED", event.get("event_type"));
        assertEquals("withdrawal-process", event.get("destination"));
        assertEquals(r.id().toString(), event.get("withdrawal_id"));
        assertEquals(1, event.get("schema_version"));
        assertEquals(0, event.get("attempts"));
        assertNull(event.get("published_at"));
        assertEquals(
                1,
                jdbc.queryForObject(
                                "SELECT count(*) FROM outbox_event e CROSS JOIN LATERAL"
                                        + " jsonb_object_keys(e.payload) k",
                                Integer.class)
                        .intValue());
    }

    @Test
    public void outboxInsertFailureRollsBackWalletRequestAndAttempt() {
        jdbc.execute(
                "ALTER TABLE outbox_event ADD CHECK (event_type <>"
                        + " 'WITHDRAWAL_PROCESS_REQUESTED')");
        assertThrows(DataIntegrityViolationException.class, () -> service().create(command()));
        assertRolledBack();
    }

    @Test
    public void outerRollbackAlsoRemovesTheAlreadyInsertedOutbox() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        tx.execute(
                                s -> {
                                    service().create(command());
                                    assertEquals(
                                            1,
                                            jdbc.queryForObject(
                                                            "SELECT count(*) FROM outbox_event",
                                                            Integer.class)
                                                    .intValue());
                                    throw new IllegalStateException("outer transaction failed");
                                }));
        assertRolledBack();
    }

    @Test
    public void writerRequiresExistingTransactionAndSupportsOtherBusinessTypes() {
        var event =
                OutboxEvent.create(
                        "PAYMENT",
                        "payment-1",
                        "PAYMENT_COMPLETED",
                        1,
                        "payment-completed",
                        Map.of("paymentId", "payment-1"));
        assertThrows(
                org.springframework.transaction.IllegalTransactionStateException.class,
                () -> writer.append(event));
        tx.execute(
                s -> {
                    writer.append(event);
                    return null;
                });
        assertEquals(
                "PAYMENT",
                jdbc.queryForObject("SELECT aggregate_type FROM outbox_event", String.class));
    }

    @Test
    public void duplicateEventIdCannotOverwritePersistedEvent() {
        var event =
                OutboxEvent.create(
                        "PAYMENT",
                        "p1",
                        "PAYMENT_COMPLETED",
                        1,
                        "payment-completed",
                        Map.of("paymentId", "p1"));
        tx.execute(
                s -> {
                    writer.append(event);
                    return null;
                });
        var duplicate =
                new OutboxEvent(
                        event.id(),
                        "PAYMENT",
                        "p2",
                        "PAYMENT_COMPLETED",
                        1,
                        "payment-completed",
                        Map.of("paymentId", "p2"),
                        event.createdAt());
        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        tx.execute(
                                s -> {
                                    writer.append(duplicate);
                                    return null;
                                }));
        tx.execute(
                s -> {
                    var saved = em.find(OutboxEventEntity.class, event.id());
                    assertEquals("p1", saved.getAggregateId());
                    assertEquals(0, saved.getAttempts());
                    assertEquals(saved.getCreatedAt(), saved.getAvailableAt());
                    assertNull(saved.getPublishedAt());
                    assertNull(saved.getLastError());
                    return null;
                });
        assertEquals(
                1,
                jdbc.queryForObject("SELECT count(*) FROM outbox_event", Integer.class).intValue());
    }
}
