package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.UserEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.WalletEntity;
import org.hibernate.query.results.internal.Builders;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    private WalletMapper walletMapper;

    public UserMapper(WalletMapper walletMapper){
        this.walletMapper = walletMapper;
    }

    public UserEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }

        UserEntity userEntity = new UserEntity();

        userEntity.setId(domain.getId());
        userEntity.setDob(domain.getDob());
        userEntity.setEmail(domain.getEmail());
        userEntity.setFullName(domain.getFullName());
        userEntity.setStatus(domain.getStatus());
        userEntity.setPhoneNumber(domain.getPhoneNumber());
        userEntity.setCreatedAt(domain.getCreatedAt());
        userEntity.setUpdatedAt(domain.getUpdatedAt());

        return userEntity;
    }

    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return new User(
                entity.getId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getDob(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
