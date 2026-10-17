package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.domain.model.entity.Role;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserMapper {

    private final WalletMapper walletMapper;
    private final RoleMapper roleMapper;

    public UserMapper(WalletMapper walletMapper, RoleMapper roleMapper) {
        this.walletMapper = walletMapper;
        this.roleMapper = roleMapper;
    }

    public UserEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }

        UserEntity userEntity = new UserEntity();

        userEntity.setId(domain.getId());
        userEntity.setDob(domain.getDob());
        userEntity.setEmail(domain.getEmail());
        userEntity.setPassword(domain.getPassword());
        userEntity.setFullName(domain.getFullName());
        userEntity.setStatus(domain.getStatus());
        userEntity.setPhoneNumber(domain.getPhoneNumber());
        userEntity.setRoles(roleMapper.toEntitySet(domain.getRoles()));
        userEntity.setCreatedAt(domain.getCreatedAt());
        userEntity.setUpdatedAt(domain.getUpdatedAt());

        return userEntity;
    }

    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        Set<Role> roles = roleMapper.toDomainSet(entity.getRoles());

        return new User(
                entity.getId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getPassword(),
                roles,
                entity.getPhoneNumber(),
                entity.getDob(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
