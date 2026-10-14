package com.tyson.digitalwallet.ddd.application.usecase.beneficiary.impl;

import com.tyson.digitalwallet.ddd.application.exception.UserNotFoundException;
import com.tyson.digitalwallet.ddd.application.exception.WalletNotFoundException;
import com.tyson.digitalwallet.ddd.application.usecase.beneficiary.BeneficiaryUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.beneficiary.command.AddBeneficiaryCommand;
import com.tyson.digitalwallet.ddd.application.usecase.beneficiary.command.UpdateBeneficiaryCommand;
import com.tyson.digitalwallet.ddd.application.usecase.beneficiary.response.BeneficiaryResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.Beneficiary;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.repository.BeneficiaryRepository;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.domain.service.cache.CacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class BeneficiaryUseCaseImpl implements BeneficiaryUseCase {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CacheService cacheService;

    public BeneficiaryUseCaseImpl(
            BeneficiaryRepository beneficiaryRepository,
            UserRepository userRepository,
            WalletRepository walletRepository,
            CacheService cacheService
    ) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.cacheService = cacheService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getBeneficiaries(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        String cacheKey = "cache:beneficiary:" + userId;
        BeneficiaryResponse[] cached = cacheService.getOrSet(
                cacheKey,
                60,
                TimeUnit.MINUTES,
                BeneficiaryResponse[].class,
                () -> beneficiaryRepository.findAllByUserId(userId).stream()
                        .map(BeneficiaryResponse::from)
                        .toArray(BeneficiaryResponse[]::new)
        );

        return cached != null ? Arrays.asList(cached) : Collections.emptyList();
    }

    @Override
    @Transactional
    public BeneficiaryResponse addBeneficiary(AddBeneficiaryCommand command) {
        UUID userId = command.userId();
        UUID beneficiaryUserId = command.beneficiaryUserId();
        UUID beneficiaryWalletId = command.beneficiaryWalletId();

        if (userId == null || beneficiaryUserId == null || beneficiaryWalletId == null) {
            throw new com.tyson.digitalwallet.ddd.application.exception.AppException(
                    com.tyson.digitalwallet.ddd.application.exception.ErrorCode.INVALID_RECIPIENT_IDENTIFIER,
                    "Beneficiary information is incomplete"
            );
        }

        if (userId.equals(beneficiaryUserId)) {
            throw new com.tyson.digitalwallet.ddd.application.exception.AppException(
                    com.tyson.digitalwallet.ddd.application.exception.ErrorCode.CANNOT_ADD_SELF_AS_BENEFICIARY,
                    "Cannot add yourself as a beneficiary"
            );
        }

        if (beneficiaryRepository.existsByUserIdAndBeneficiaryUserId(userId, beneficiaryUserId)) {
            throw new com.tyson.digitalwallet.ddd.application.exception.AppException(
                    com.tyson.digitalwallet.ddd.application.exception.ErrorCode.BENEFICIARY_ALREADY_EXISTS,
                    "Recipient is already in your beneficiaries"
            );
        }

        User targetUser = userRepository.findById(beneficiaryUserId)
                .orElseThrow(() -> new UserNotFoundException("Beneficiary user not found"));

        walletRepository.findById(beneficiaryWalletId)
                .orElseThrow(() -> new WalletNotFoundException("Beneficiary wallet not found"));

        Beneficiary beneficiary = Beneficiary.create(
                userId,
                beneficiaryUserId,
                beneficiaryWalletId,
                command.nickname()
        );

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        saved.setFullName(targetUser.getFullName());
        saved.setPhoneNumber(targetUser.getPhoneNumber());
        saved.setEmail(targetUser.getEmail());

        // Invalidate cache
        cacheService.delete("cache:beneficiary:" + userId);

        return BeneficiaryResponse.from(saved);
    }

    @Override
    @Transactional
    public BeneficiaryResponse updateBeneficiary(UpdateBeneficiaryCommand command) {
        Beneficiary beneficiary = beneficiaryRepository.findById(command.beneficiaryId())
                .orElseThrow(() -> new com.tyson.digitalwallet.ddd.application.exception.AppException(
                        com.tyson.digitalwallet.ddd.application.exception.ErrorCode.BENEFICIARY_NOT_FOUND,
                        "Beneficiary contact not found"
                ));

        if (!beneficiary.getUserId().equals(command.userId())) {
            throw new com.tyson.digitalwallet.ddd.application.exception.AppException(
                    com.tyson.digitalwallet.ddd.application.exception.ErrorCode.FORBIDDEN_ACCESS,
                    "You are not authorized to update this beneficiary"
            );
        }

        beneficiary.updateNickname(command.nickname());
        Beneficiary updated = beneficiaryRepository.save(beneficiary);

        // Nạp thông tin người nhận nếu thiếu
        if (updated.getFullName() == null) {
            userRepository.findById(updated.getBeneficiaryUserId()).ifPresent(u -> {
                updated.setFullName(u.getFullName());
                updated.setPhoneNumber(u.getPhoneNumber());
                updated.setEmail(u.getEmail());
            });
        }

        // Invalidate cache
        cacheService.delete("cache:beneficiary:" + command.userId());

        return BeneficiaryResponse.from(updated);
    }

    @Override
    @Transactional
    public void deleteBeneficiary(UUID beneficiaryId, UUID userId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new com.tyson.digitalwallet.ddd.application.exception.AppException(
                        com.tyson.digitalwallet.ddd.application.exception.ErrorCode.BENEFICIARY_NOT_FOUND,
                        "Beneficiary contact not found"
                ));

        if (!beneficiary.getUserId().equals(userId)) {
            throw new com.tyson.digitalwallet.ddd.application.exception.AppException(
                    com.tyson.digitalwallet.ddd.application.exception.ErrorCode.FORBIDDEN_ACCESS,
                    "You are not authorized to delete this beneficiary"
            );
        }

        beneficiaryRepository.delete(beneficiary);

        // Invalidate cache
        cacheService.delete("cache:beneficiary:" + userId);
    }
}
