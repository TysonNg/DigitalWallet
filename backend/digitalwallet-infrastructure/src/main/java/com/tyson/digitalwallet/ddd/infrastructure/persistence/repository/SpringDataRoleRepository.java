package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.RoleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataRoleRepository extends JpaRepository<RoleEntity, Long> {

    @EntityGraph(attributePaths = {"authorities"})
    Optional<RoleEntity> findByName(String name);

    @Override
    @EntityGraph(attributePaths = {"authorities"})
    Optional<RoleEntity> findById(Long id);
}
