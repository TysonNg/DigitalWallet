package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.Authority;
import com.tyson.digitalwallet.ddd.domain.repository.AuthorityRepository;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.AuthorityMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.AuthorityEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class AuthorityRepositoryImpl implements AuthorityRepository {

    private final SpringDataAuthorityRepository springDataAuthorityRepository;
    private final AuthorityMapper authorityMapper;

    public AuthorityRepositoryImpl(SpringDataAuthorityRepository springDataAuthorityRepository,
                                   AuthorityMapper authorityMapper) {
        this.springDataAuthorityRepository = springDataAuthorityRepository;
        this.authorityMapper = authorityMapper;
    }

    @Override
    public Optional<Authority> findByName(String name) {
        return springDataAuthorityRepository.findByName(name).map(authorityMapper::toDomain);
    }

    @Override
    public Optional<Authority> findById(Long id) {
        return springDataAuthorityRepository.findById(id).map(authorityMapper::toDomain);
    }

    @Override
    public List<Authority> findAll() {
        return springDataAuthorityRepository.findAll().stream()
                .map(authorityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Authority save(Authority authority) {
        AuthorityEntity entity = authorityMapper.toEntity(authority);
        AuthorityEntity saved = springDataAuthorityRepository.save(entity);
        return authorityMapper.toDomain(saved);
    }
}
