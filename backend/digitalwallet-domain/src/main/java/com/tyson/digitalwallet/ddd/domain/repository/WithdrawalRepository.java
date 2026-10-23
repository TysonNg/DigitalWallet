package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.WithdrawalRequest;

import java.util.*;

public interface WithdrawalRepository {
    Optional<WithdrawalRequest> findByWalletAndKey(UUID walletId, String key);

    Optional<WithdrawalRequest> findByIdWithLock(UUID id);

    WithdrawalRequest save(WithdrawalRequest request);

    List<WithdrawalRequest> findByWallet(UUID walletId);

    List<UUID> findPendingIds();

    java.math.BigDecimal sumHeldAmount(UUID walletId);
}
