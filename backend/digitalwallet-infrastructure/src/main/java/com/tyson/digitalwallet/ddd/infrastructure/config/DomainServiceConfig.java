package com.tyson.digitalwallet.ddd.infrastructure.config;

import com.tyson.digitalwallet.ddd.domain.repository.AccountRepository;
import com.tyson.digitalwallet.ddd.domain.repository.LedgerRepository;
import com.tyson.digitalwallet.ddd.domain.repository.NotificationRepository;
import com.tyson.digitalwallet.ddd.domain.repository.PaymentOrderRepository;
import com.tyson.digitalwallet.ddd.domain.repository.TransactionRepository;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.domain.service.ledger.LedgerService;
import com.tyson.digitalwallet.ddd.domain.service.ledger.impl.LedgerServiceImpl;
import com.tyson.digitalwallet.ddd.domain.service.notification.NotificationService;
import com.tyson.digitalwallet.ddd.domain.service.notification.impl.NotificationServiceImpl;
import com.tyson.digitalwallet.ddd.domain.service.payment.PaymentService;
import com.tyson.digitalwallet.ddd.domain.service.payment.impl.PaymentServiceImpl;
import com.tyson.digitalwallet.ddd.domain.service.transaction.TransactionService;
import com.tyson.digitalwallet.ddd.domain.service.transaction.impl.TransactionServiceImpl;
import com.tyson.digitalwallet.ddd.domain.service.user.UserService;
import com.tyson.digitalwallet.ddd.domain.service.user.impl.UserServiceImpl;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.impl.WalletServiceImpl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public LedgerService ledgerService(
            AccountRepository accountRepository,
            LedgerRepository ledgerRepository,
            com.tyson.digitalwallet.ddd.domain.repository.WithdrawalRepository
                    withdrawalRepository) {
        return new LedgerServiceImpl(accountRepository, ledgerRepository, withdrawalRepository);
    }

    @Bean
    public WalletService walletService(
            WalletRepository walletRepository, TransactionRepository transactionRepository) {
        return new WalletServiceImpl(walletRepository, transactionRepository);
    }

    @Bean
    public PaymentService paymentService(PaymentOrderRepository paymentOrderRepository) {
        return new PaymentServiceImpl(paymentOrderRepository);
    }

    @Bean
    public UserService userService(UserRepository userRepository) {
        return new UserServiceImpl(userRepository);
    }

    @Bean
    public TransactionService transactionService(TransactionRepository transactionRepository) {
        return new TransactionServiceImpl(transactionRepository);
    }

    @Bean
    public NotificationService notificationService(NotificationRepository notificationRepository) {
        return new NotificationServiceImpl(notificationRepository);
    }
}
