package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.domain.model.enums.UserStatus;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {
    boolean existsByEmail(String email);

    @Override
    @EntityGraph(attributePaths = {"roles", "roles.authorities"})
    Optional<UserEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"roles", "roles.authorities"})
    Optional<UserEntity> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles", "roles.authorities"})
    Optional<UserEntity> findByPhoneNumber(String phoneNumber);

    @EntityGraph(attributePaths = {"roles", "roles.authorities"})
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.email) = LOWER(:identifier) OR u.phoneNumber = :identifier")
    Optional<UserEntity> findByIdentifier(@Param("identifier") String identifier);

    @EntityGraph(attributePaths = {"roles", "roles.authorities"})
    @Query("SELECT u FROM UserEntity u ORDER BY u.createdAt DESC")
    List<UserEntity> findAllWithRoles();

    long countByStatus(UserStatus status);
}

