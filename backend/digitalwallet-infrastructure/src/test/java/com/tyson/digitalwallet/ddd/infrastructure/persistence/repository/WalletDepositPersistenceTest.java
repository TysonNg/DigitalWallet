package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.domain.model.enums.UserStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import com.tyson.digitalwallet.ddd.domain.service.wallet.impl.WalletServiceImpl;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.TransactionMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.WalletMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.*;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.Assert.*;

public class WalletDepositPersistenceTest {
    @Test
    public void depositPersistsBalanceAndTransactionWithDomainAssignedId() {
        Configuration configuration = new Configuration()
                .addAnnotatedClass(AuthorityEntity.class)
                .addAnnotatedClass(RoleEntity.class)
                .addAnnotatedClass(UserEntity.class)
                .addAnnotatedClass(WalletEntity.class)
                .addAnnotatedClass(TransactionEntity.class)
                .addAnnotatedClass(IdempotencyKeyEntity.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:deposit;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop");

        try (var factory = configuration.buildSessionFactory(); var session = factory.openSession()) {
            var transaction = session.beginTransaction();
            try {
                UserEntity user = new UserEntity();
                user.setId(UUID.randomUUID());
                user.setFullName("Deposit test");
                user.setEmail("deposit@example.com");
                user.setPhoneNumber("0900000000");
                user.setPassword("test-only");
                user.setDob(LocalDate.of(2000, 1, 1));
                user.setStatus(UserStatus.ACTIVE);
                session.persist(user);
                UUID walletId = UUID.randomUUID();
                session.persist(new WalletEntity(walletId, BigDecimal.ZERO, "VND",
                        WalletStatus.ACTIVE, null, null, user));
                session.flush();
                session.clear();

                JpaRepositoryFactory repositories = new JpaRepositoryFactory(session);
                var wallets = new WalletRepositoryImpl(
                        repositories.getRepository(SpringDataWalletRepository.class), new WalletMapper());
                var transactions = new TransactionRepositoryImpl(
                        repositories.getRepository(SpringDataTransactionRepository.class), new TransactionMapper());
                var service = new WalletServiceImpl(wallets, transactions);

                var result = service.deposit(walletId, new BigDecimal("500000"));
                UUID transactionId = result.transaction().getId();
                assertNotNull(transactionId);
                session.flush();
                session.clear();

                assertEquals(0, new BigDecimal("500000").compareTo(
                        session.find(WalletEntity.class, walletId).getBalance()));
                TransactionEntity saved = session.find(TransactionEntity.class, transactionId);
                assertNotNull("The deposit transaction must be stored under its domain ID", saved);
                assertEquals(0, new BigDecimal("500000").compareTo(saved.getAmount()));
                assertEquals(walletId, saved.getReceiverWallet().getId());
                assertEquals(1L, session.createQuery("select count(t) from TransactionEntity t", Long.class)
                        .getSingleResult().longValue());
            } finally {
                transaction.rollback();
            }
        }
    }
}