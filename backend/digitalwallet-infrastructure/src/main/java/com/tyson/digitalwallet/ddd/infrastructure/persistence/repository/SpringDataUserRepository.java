package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {
    boolean existsByEmail(String email);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.email) = LOWER(:identifier) OR u.phoneNumber = :identifier")
    Optional<UserEntity> findByIdentifier(@Param("identifier") String identifier);
}
