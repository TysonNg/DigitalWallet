package com.tyson.digitalwallet.ddd.domain.service.wallet;

import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.DepositResult;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.TransferResult;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.WithdrawResult;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletService {
    Wallet createWallet(UUID userId, String currency);

    DepositResult deposit(UUID walletId, BigDecimal amount);

    WithdrawResult withdraw(UUID walletId, BigDecimal amount);

    TransferResult transfer(UUID senderWalletId, UUID receiverWalletId, BigDecimal amount, String description);
}
