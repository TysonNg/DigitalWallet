package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.domain.model.entity.Beneficiary;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.BeneficiaryEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.UserEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.WalletEntity;
import org.springframework.stereotype.Component;

@Component
public class BeneficiaryMapper {

    public BeneficiaryEntity toEntity(Beneficiary domain) {
        if (domain == null) {
            return null;
        }

        UserEntity user = new UserEntity();
        user.setId(domain.getUserId());

        UserEntity beneficiaryUser = new UserEntity();
        beneficiaryUser.setId(domain.getBeneficiaryUserId());

        WalletEntity beneficiaryWallet = new WalletEntity();
        beneficiaryWallet.setId(domain.getBeneficiaryWalletId());

        return new BeneficiaryEntity(
                domain.getId(),
                user,
                beneficiaryUser,
                beneficiaryWallet,
                domain.getNickname(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public Beneficiary toDomain(BeneficiaryEntity entity) {
        if (entity == null) {
            return null;
        }

        String fullName = entity.getBeneficiaryUser() != null ? entity.getBeneficiaryUser().getFullName() : null;
        String phoneNumber = entity.getBeneficiaryUser() != null ? entity.getBeneficiaryUser().getPhoneNumber() : null;
        String email = entity.getBeneficiaryUser() != null ? entity.getBeneficiaryUser().getEmail() : null;

        return new Beneficiary(
                entity.getId(),
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getBeneficiaryUser() != null ? entity.getBeneficiaryUser().getId() : null,
                entity.getBeneficiaryWallet() != null ? entity.getBeneficiaryWallet().getId() : null,
                entity.getNickname(),
                fullName,
                phoneNumber,
                email,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
