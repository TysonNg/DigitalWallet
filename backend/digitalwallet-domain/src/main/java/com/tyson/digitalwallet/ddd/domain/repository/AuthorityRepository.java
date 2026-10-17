package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.Authority;

import java.util.List;
import java.util.Optional;

public interface AuthorityRepository {

    Optional<Authority> findByName(String name);

    Optional<Authority> findById(Long id);

    List<Authority> findAll();

    Authority save(Authority authority);
}
