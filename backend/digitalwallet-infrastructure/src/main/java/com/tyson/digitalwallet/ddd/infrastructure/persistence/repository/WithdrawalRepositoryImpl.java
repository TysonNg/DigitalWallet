package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.WithdrawalRequest;
import com.tyson.digitalwallet.ddd.domain.repository.WithdrawalRepository;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.WithdrawalMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.WithdrawalRequestEntity;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class WithdrawalRepositoryImpl implements WithdrawalRepository {

    private static final Set<com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus>
            PENDING_STATUSES =
                    Set.of(
                            com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus.PENDING,
                            com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus
                                    .PROCESSING,
                            com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus
                                    .SUCCEEDED);

    private final SpringDataWithdrawalRepository springDataRepository;
    private final WithdrawalMapper mapper;

    public WithdrawalRepositoryImpl(
            SpringDataWithdrawalRepository springDataRepository, WithdrawalMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<WithdrawalRequest> findByWalletAndKey(UUID walletId, String key) {
        return springDataRepository
                .findByWalletIdAndIdempotencyKey(walletId, key)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<WithdrawalRequest> findByIdWithLock(UUID id) {
        return springDataRepository.findByIdWithLock(id).map(mapper::toDomain);
    }

    @Override
    public WithdrawalRequest save(WithdrawalRequest request) {
        WithdrawalRequestEntity entity = mapper.toEntity(request);
        WithdrawalRequestEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<WithdrawalRequest> findByWallet(UUID walletId) {
        return springDataRepository.findTop50ByWalletIdOrderByCreatedAtDesc(walletId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<UUID> findPendingIds() {
        return springDataRepository.findPendingIds(
                PENDING_STATUSES,
                java.time.Instant.now().minusSeconds(3600),
                PageRequest.of(0, 100));
    }

    @Override
    public java.math.BigDecimal sumHeldAmount(UUID walletId) {
        return springDataRepository.sumHeldAmount(
                walletId,
                Set.of(
                        com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus.PENDING,
                        com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus.PROCESSING,
                        com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus
                                .REVIEW_REQUIRED));
    }
}
