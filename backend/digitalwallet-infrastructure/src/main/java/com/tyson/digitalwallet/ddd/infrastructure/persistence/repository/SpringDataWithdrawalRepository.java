package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.WithdrawalRequestEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataWithdrawalRepository
        extends JpaRepository<WithdrawalRequestEntity, UUID> {

    Optional<WithdrawalRequestEntity> findByWalletIdAndIdempotencyKey(
            UUID walletId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WithdrawalRequestEntity w WHERE w.id = :id")
    Optional<WithdrawalRequestEntity> findByIdWithLock(@Param("id") UUID id);

    List<WithdrawalRequestEntity> findTop50ByWalletIdOrderByCreatedAtDesc(UUID walletId);

    @Query(
            "SELECT w.id FROM WithdrawalRequestEntity w WHERE w.status IN :statuses AND (w.status"
                + " <> com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus.SUCCEEDED OR"
                + " w.updatedAt <= :successCutoff) ORDER BY w.updatedAt ASC")
    List<UUID> findPendingIds(
            @Param("statuses")
                    Collection<com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus>
                            statuses,
            @Param("successCutoff") java.time.Instant successCutoff,
            Pageable pageable);

    @Query(
            "SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawalRequestEntity w WHERE w.walletId ="
                    + " :walletId AND w.status IN :statuses")
    java.math.BigDecimal sumHeldAmount(
            @Param("walletId") UUID walletId,
            @Param("statuses")
                    Collection<com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus>
                            statuses);

    boolean existsByWalletIdAndStatusIn(
            UUID walletId,
            Collection<com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus> statuses);
}
