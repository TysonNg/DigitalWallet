package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.domain.model.entity.Authority;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.AuthorityEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuthorityMapper {

    public AuthorityEntity toEntity(Authority domain) {
        if (domain == null) {
            return null;
        }

        AuthorityEntity entity = new AuthorityEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public Authority toDomain(AuthorityEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Authority(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    public Set<AuthorityEntity> toEntitySet(Set<Authority> domains) {
        if (domains == null || domains.isEmpty()) {
            return Collections.emptySet();
        }
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toSet());
    }

    public Set<Authority> toDomainSet(Set<AuthorityEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptySet();
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toSet());
    }
}
