package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import static org.junit.Assert.*;

import com.tyson.digitalwallet.ddd.domain.model.entity.*;
import com.tyson.digitalwallet.ddd.domain.model.enums.*;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.*;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.*;

import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.math.BigDecimal;
import java.util.*;

public class WithdrawalPersistenceTest {
    @Test
    public void restartRestoresAttemptCheckpointsAndHoldQueries() {
        var config =
                new Configuration()
                        .addAnnotatedClass(WithdrawalRequestEntity.class)
                        .addAnnotatedClass(PayoutAttemptEntity.class)
                        .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                        .setProperty(
                                "hibernate.connection.url",
                                "jdbc:h2:mem:withdrawal;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
                        .setProperty("hibernate.hbm2ddl.auto", "create-drop");
        UUID wallet = UUID.randomUUID(), user = UUID.randomUUID();
        var r =
                WithdrawalRequest.create(
                        wallet,
                        user,
                        new BigDecimal("200000"),
                        new BigDecimal("1000000"),
                        "VND",
                        "test-bank:SUCCESS",
                        "persistent-key");
        var a =
                PayoutAttempt.create(r.getId(), "STRIPE", 800, "USD")
                        .checkpoint("account_reference", "acct_test")
                        .checkpoint("transfer_started", "true")
                        .withStatus(PayoutAttemptStatus.UNKNOWN, "timeout");
        try (var factory = config.buildSessionFactory()) {
            try (var session = factory.openSession()) {
                var tx = session.beginTransaction();
                var repositories = new JpaRepositoryFactory(session);
                var withdrawals =
                        new WithdrawalRepositoryImpl(
                                repositories.getRepository(SpringDataWithdrawalRepository.class),
                                new WithdrawalMapper());
                var attempts =
                        new PayoutAttemptRepositoryImpl(
                                repositories.getRepository(SpringDataPayoutAttemptRepository.class),
                                new PayoutAttemptMapper());
                withdrawals.save(r);
                attempts.save(a);
                tx.commit();
            }
            try (var session = factory.openSession()) {
                var tx = session.beginTransaction();
                var repositories = new JpaRepositoryFactory(session);
                var withdrawals =
                        new WithdrawalRepositoryImpl(
                                repositories.getRepository(SpringDataWithdrawalRepository.class),
                                new WithdrawalMapper());
                var attempts =
                        new PayoutAttemptRepositoryImpl(
                                repositories.getRepository(SpringDataPayoutAttemptRepository.class),
                                new PayoutAttemptMapper());
                assertEquals(
                        0, new BigDecimal("200000").compareTo(withdrawals.sumHeldAmount(wallet)));
                assertEquals(List.of(r.getId()), withdrawals.findPendingIds());
                assertTrue(withdrawals.existsHeldWithdrawalByWalletId(wallet));
                var restored = attempts.findByWithdrawalId(r.getId()).orElseThrow();
                assertEquals(a.getId(), restored.getId());
                assertEquals(PayoutAttemptStatus.UNKNOWN, restored.getStatus());
                assertEquals("true", restored.getMetadata().get("transfer_started"));
                var stored = withdrawals.findByIdWithLock(r.getId()).orElseThrow();
                stored.fail("rejected");
                withdrawals.save(stored);
                session.flush();
                assertEquals(0, BigDecimal.ZERO.compareTo(withdrawals.sumHeldAmount(wallet)));
                assertFalse(withdrawals.existsHeldWithdrawalByWalletId(wallet));
                var completed =
                        WithdrawalRequest.create(
                                wallet,
                                user,
                                new BigDecimal("25000"),
                                new BigDecimal("1000000"),
                                "VND",
                                "test-bank:SUCCESS",
                                "completed-key");
                completed.complete(UUID.randomUUID());
                withdrawals.save(completed);
                session.flush();
                assertFalse(
                        "Recently successful payouts must not be polled every 15 seconds",
                        withdrawals.findPendingIds().contains(completed.getId()));
                session.createMutationQuery(
                                "update WithdrawalRequestEntity w set w.updatedAt = :old where w.id"
                                        + " = :id")
                        .setParameter("old", java.time.Instant.now().minusSeconds(7200))
                        .setParameter("id", completed.getId())
                        .executeUpdate();
                assertTrue(
                        "Older successful payouts are reconciled for late returns",
                        withdrawals.findPendingIds().contains(completed.getId()));
                tx.rollback();
            }
        }
    }
}
