package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {

    Optional<Role> findByName(String name);

    Optional<Role> findById(Long id);

    List<Role> findAll();

    Role save(Role role);
}
