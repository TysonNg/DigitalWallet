package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }

        return new UserEntity(
                domain.getId(),
                domain.getFullName(),
                domain.getEmail(),
                domain.getPhoneNumber(),
                domain.getDob(),
                domain.getStatus(),
                domain.getCreatedAt()
        );
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
                entity.getCreatedAt()
        );
    }
}
