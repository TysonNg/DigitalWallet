package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.WithdrawalRequest;

import java.util.*;

public interface WithdrawalRepository {
    Optional<WithdrawalRequest> findByWalletAndKey(UUID walletId, String key);

    Optional<WithdrawalRequest> findByIdWithLock(UUID id);

    boolean existsHeldWithdrawalByWalletId(UUID walletId);

    WithdrawalRequest save(WithdrawalRequest request);

    List<WithdrawalRequest> findByWallet(UUID walletId);

    List<UUID> findPendingIds();

    /** Started payouts only; fresh requests remain owned by the message consumer. */
    List<UUID> findRecoveryIds();

    java.math.BigDecimal sumHeldAmount(UUID walletId);
}
