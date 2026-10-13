package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.BeneficiaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataBeneficiaryRepository extends JpaRepository<BeneficiaryEntity, UUID> {

    @Query("SELECT b FROM BeneficiaryEntity b " +
           "JOIN FETCH b.beneficiaryUser " +
           "JOIN FETCH b.beneficiaryWallet " +
           "WHERE b.user.id = :userId " +
           "ORDER BY b.createdAt DESC")
    List<BeneficiaryEntity> findAllByUserIdWithDetails(@Param("userId") UUID userId);

    @Query("SELECT b FROM BeneficiaryEntity b WHERE b.user.id = :userId AND b.beneficiaryUser.id = :beneficiaryUserId")
    Optional<BeneficiaryEntity> findByUserIdAndBeneficiaryUserId(
            @Param("userId") UUID userId,
            @Param("beneficiaryUserId") UUID beneficiaryUserId
    );

    @Query("SELECT COUNT(b) > 0 FROM BeneficiaryEntity b WHERE b.user.id = :userId AND b.beneficiaryUser.id = :beneficiaryUserId")
    boolean existsByUserIdAndBeneficiaryUserId(
            @Param("userId") UUID userId,
            @Param("beneficiaryUserId") UUID beneficiaryUserId
    );
}
