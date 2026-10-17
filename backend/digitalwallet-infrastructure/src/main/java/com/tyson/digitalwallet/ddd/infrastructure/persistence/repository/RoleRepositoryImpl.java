package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.Role;
import com.tyson.digitalwallet.ddd.domain.repository.RoleRepository;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.RoleMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.RoleEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final SpringDataRoleRepository springDataRoleRepository;
    private final RoleMapper roleMapper;

    public RoleRepositoryImpl(SpringDataRoleRepository springDataRoleRepository, RoleMapper roleMapper) {
        this.springDataRoleRepository = springDataRoleRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    public Optional<Role> findByName(String name) {
        return springDataRoleRepository.findByName(name).map(roleMapper::toDomain);
    }

    @Override
    public Optional<Role> findById(Long id) {
        return springDataRoleRepository.findById(id).map(roleMapper::toDomain);
    }

    @Override
    public List<Role> findAll() {
        return springDataRoleRepository.findAll().stream()
                .map(roleMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Role save(Role role) {
        RoleEntity entity = roleMapper.toEntity(role);
        RoleEntity saved = springDataRoleRepository.save(entity);
        return roleMapper.toDomain(saved);
    }
}
