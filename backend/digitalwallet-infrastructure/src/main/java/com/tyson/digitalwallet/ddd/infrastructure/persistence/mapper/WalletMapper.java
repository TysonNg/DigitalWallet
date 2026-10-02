package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.UserEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.WalletEntity;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {
    public WalletEntity toEntity(Wallet domain){
        if(domain == null) {
            return null;
        }

        UserEntity userEntity = null;
        if(domain.getUserId() != null) {
            userEntity = new UserEntity();
            userEntity.setId(domain.getUserId());
        }

        return new WalletEntity(
                domain.getId(),
                domain.getBalance(),
                domain.getCurrency(),
                domain.getStatus(),
                domain.getCreated_at(),
                domain.getUpdated_at(),
                userEntity
        );
    }

    public Wallet toDomain(WalletEntity walletEntity) {
        if(walletEntity == null) {
            return null;
        }

        return new Wallet(
                walletEntity.getId(),
                walletEntity.getUser().getId(),
                walletEntity.getBalance(),
                walletEntity.getCurrency(),
                walletEntity.getStatus(),
                walletEntity.getCreatedAt(),
                walletEntity.getUpdatedAt()
        );
    }
}
