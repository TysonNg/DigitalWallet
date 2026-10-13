package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.Beneficiary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BeneficiaryRepository {
    List<Beneficiary> findAllByUserId(UUID userId);

    Optional<Beneficiary> findById(UUID id);

    Optional<Beneficiary> findByUserIdAndBeneficiaryUserId(UUID userId, UUID beneficiaryUserId);

    boolean existsByUserIdAndBeneficiaryUserId(UUID userId, UUID beneficiaryUserId);

    Beneficiary save(Beneficiary beneficiary);

    void delete(Beneficiary beneficiary);
}
