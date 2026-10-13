package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.Beneficiary;
import com.tyson.digitalwallet.ddd.domain.repository.BeneficiaryRepository;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.BeneficiaryMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.BeneficiaryEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BeneficiaryRepositoryImpl implements BeneficiaryRepository {

    private final SpringDataBeneficiaryRepository springDataBeneficiaryRepository;
    private final BeneficiaryMapper beneficiaryMapper;

    public BeneficiaryRepositoryImpl(
            SpringDataBeneficiaryRepository springDataBeneficiaryRepository,
            BeneficiaryMapper beneficiaryMapper
    ) {
        this.springDataBeneficiaryRepository = springDataBeneficiaryRepository;
        this.beneficiaryMapper = beneficiaryMapper;
    }

    @Override
    public List<Beneficiary> findAllByUserId(UUID userId) {
        return springDataBeneficiaryRepository.findAllByUserIdWithDetails(userId).stream()
                .map(beneficiaryMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Beneficiary> findById(UUID id) {
        return springDataBeneficiaryRepository.findById(id)
                .map(beneficiaryMapper::toDomain);
    }

    @Override
    public Optional<Beneficiary> findByUserIdAndBeneficiaryUserId(UUID userId, UUID beneficiaryUserId) {
        return springDataBeneficiaryRepository.findByUserIdAndBeneficiaryUserId(userId, beneficiaryUserId)
                .map(beneficiaryMapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndBeneficiaryUserId(UUID userId, UUID beneficiaryUserId) {
        return springDataBeneficiaryRepository.existsByUserIdAndBeneficiaryUserId(userId, beneficiaryUserId);
    }

    @Override
    public Beneficiary save(Beneficiary beneficiary) {
        BeneficiaryEntity entity = beneficiaryMapper.toEntity(beneficiary);
        BeneficiaryEntity saved = springDataBeneficiaryRepository.save(entity);
        return beneficiaryMapper.toDomain(saved);
    }

    @Override
    public void delete(Beneficiary beneficiary) {
        springDataBeneficiaryRepository.deleteById(beneficiary.getId());
    }
}
