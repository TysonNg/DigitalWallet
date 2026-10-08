package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.IdempotencyKey;
import com.tyson.digitalwallet.ddd.domain.repository.IdempotencyKeyRepository;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.IdempotencyKeyMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.IdempotencyKeyEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class IdempotencyKeyRepositoryImpl implements IdempotencyKeyRepository {

    private final SpringDataIdempotencyKeyRepository springDataIdempotencyKeyRepository;
    private final IdempotencyKeyMapper idempotencyKeyMapper;

    public IdempotencyKeyRepositoryImpl(
            SpringDataIdempotencyKeyRepository springDataIdempotencyKeyRepository,
            IdempotencyKeyMapper idempotencyKeyMapper
    ) {
        this.springDataIdempotencyKeyRepository = springDataIdempotencyKeyRepository;
        this.idempotencyKeyMapper = idempotencyKeyMapper;
    }

    @Override
    public Optional<IdempotencyKey> findByUserIdAndKey(UUID userId, String idempotencyKey) {
        return springDataIdempotencyKeyRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(idempotencyKeyMapper::toDomain);
    }

    @Override
    public IdempotencyKey save(IdempotencyKey idempotencyKey) {
        IdempotencyKeyEntity entity = idempotencyKeyMapper.toEntity(idempotencyKey);
        IdempotencyKeyEntity saved = springDataIdempotencyKeyRepository.save(entity);
        return idempotencyKeyMapper.toDomain(saved);
    }
}
