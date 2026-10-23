package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tyson.digitalwallet.ddd.domain.model.entity.WithdrawalRequest;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.WithdrawalMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.WithdrawalRequestEntity;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WithdrawalRepositoryImplTest {

    private SpringDataWithdrawalRepository springDataRepository;
    private WithdrawalMapper mapper;
    private WithdrawalRepositoryImpl repository;

    private final UUID id = UUID.randomUUID();
    private final UUID walletId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Before
    public void setUp() {
        springDataRepository = mock(SpringDataWithdrawalRepository.class);
        mapper = new WithdrawalMapper();
        repository = new WithdrawalRepositoryImpl(springDataRepository, mapper);
    }

    private WithdrawalRequest sampleDomain() {
        return new WithdrawalRequest(
                id,
                walletId,
                userId,
                new BigDecimal("250000"),
                new BigDecimal("1000000"),
                "VND",
                "test-bank:SUCCESS",
                "key-12345678",
                com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus.PENDING,
                null,
                null,
                Instant.now(),
                Instant.now());
    }

    @Test
    public void saveConvertsAndDelegatesToSpringDataRepository() {
        WithdrawalRequest domain = sampleDomain();
        WithdrawalRequestEntity entity = mapper.toEntity(domain);
        when(springDataRepository.save(any(WithdrawalRequestEntity.class))).thenReturn(entity);

        WithdrawalRequest result = repository.save(domain);

        assertNotNull(result);
        assertEquals(domain.getId(), result.getId());
        assertEquals(domain.getWalletId(), result.getWalletId());
        assertEquals(domain.getAmount(), result.getAmount());
        assertEquals(domain.getIdempotencyKey(), result.getIdempotencyKey());
        verify(springDataRepository).save(any(WithdrawalRequestEntity.class));
    }

    @Test
    public void findByIdWithLockMapsEntityToDomain() {
        WithdrawalRequest domain = sampleDomain();
        WithdrawalRequestEntity entity = mapper.toEntity(domain);
        when(springDataRepository.findByIdWithLock(id)).thenReturn(Optional.of(entity));

        Optional<WithdrawalRequest> result = repository.findByIdWithLock(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        assertEquals(walletId, result.get().getWalletId());
    }

    @Test
    public void findByWalletAndKeyMapsEntityToDomain() {
        WithdrawalRequest domain = sampleDomain();
        WithdrawalRequestEntity entity = mapper.toEntity(domain);
        when(springDataRepository.findByWalletIdAndIdempotencyKey(walletId, "key-12345678"))
                .thenReturn(Optional.of(entity));

        Optional<WithdrawalRequest> result =
                repository.findByWalletAndKey(walletId, "key-12345678");

        assertTrue(result.isPresent());
        assertEquals("key-12345678", result.get().getIdempotencyKey());
    }

    @Test
    public void findPendingIdsDelegatesToSpringDataRepository() {
        List<UUID> pending = List.of(id);
        when(springDataRepository.findPendingIds(
                        any(Collection.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(pending);

        List<UUID> result = repository.findPendingIds();

        assertEquals(1, result.size());
        assertEquals(id, result.get(0));
    }
}
