package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.domain.model.entity.Authority;
import com.tyson.digitalwallet.ddd.domain.model.entity.Role;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.RoleEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RoleMapper {

    private final AuthorityMapper authorityMapper;

    public RoleMapper(AuthorityMapper authorityMapper) {
        this.authorityMapper = authorityMapper;
    }

    public RoleEntity toEntity(Role domain) {
        if (domain == null) {
            return null;
        }

        RoleEntity entity = new RoleEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setAuthorities(authorityMapper.toEntitySet(domain.getAuthorities()));
        return entity;
    }

    public Role toDomain(RoleEntity entity) {
        if (entity == null) {
            return null;
        }

        Set<Authority> authorities = authorityMapper.toDomainSet(entity.getAuthorities());

        return new Role(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt(),
                authorities
        );
    }

    public Set<RoleEntity> toEntitySet(Set<Role> domains) {
        if (domains == null || domains.isEmpty()) {
            return Collections.emptySet();
        }
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toSet());
    }

    public Set<Role> toDomainSet(Set<RoleEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptySet();
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toSet());
    }
}
