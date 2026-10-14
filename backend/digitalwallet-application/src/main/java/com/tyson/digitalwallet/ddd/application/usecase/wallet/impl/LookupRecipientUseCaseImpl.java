package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.UserNotActiveException;
import com.tyson.digitalwallet.ddd.application.exception.UserNotFoundException;
import com.tyson.digitalwallet.ddd.application.exception.WalletNotFoundException;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.LookupRecipientUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.RecipientLookupResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.model.enums.UserStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.domain.service.cache.CacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LookupRecipientUseCaseImpl implements LookupRecipientUseCase {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CacheService cacheService;

    public LookupRecipientUseCaseImpl(
            UserRepository userRepository,
            WalletRepository walletRepository,
            CacheService cacheService
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.cacheService = cacheService;
    }

    @Override
    @Transactional(readOnly = true)
    public RecipientLookupResponse lookup(UUID currentUserId, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new com.tyson.digitalwallet.ddd.application.exception.AppException(
                    com.tyson.digitalwallet.ddd.application.exception.ErrorCode.INVALID_RECIPIENT_IDENTIFIER,
                    "Recipient identifier must not be blank"
            );
        }

        String query = identifier.trim();
        String cacheKey = "cache:recipient:" + query.toLowerCase();

        // 1. Tìm từ cache hoặc query DB qua helper getOrSet (TTL 10 phút)
        RecipientLookupResponse response = cacheService.getOrSet(
                cacheKey,
                10,
                TimeUnit.MINUTES,
                RecipientLookupResponse.class,
                () -> queryRecipientFromDb(query)
        );

        // 2. Kiểm tra không được chuyển cho chính mình (kiểm tra sau khi lấy được thông tin)
        if (currentUserId != null && response.userId().equals(currentUserId)) {
            throw new com.tyson.digitalwallet.ddd.application.exception.AppException(
                    com.tyson.digitalwallet.ddd.application.exception.ErrorCode.CANNOT_TRANSFER_TO_SELF,
                    "Cannot transfer money to yourself"
            );
        }

        return response;
    }

    private RecipientLookupResponse queryRecipientFromDb(String query) {
        User recipientUser = null;
        Wallet recipientWallet = null;

        // 1. Kiểm tra nếu query là UUID ví trực tiếp
        if (query.length() == 36 && query.contains("-")) {
            try {
                UUID possibleWalletId = UUID.fromString(query);
                Optional<Wallet> walletOpt = walletRepository.findById(possibleWalletId);
                if (walletOpt.isPresent()) {
                    recipientWallet = walletOpt.get();
                    recipientUser = userRepository.findById(recipientWallet.getUserId())
                            .orElse(null);
                }
            } catch (IllegalArgumentException ignored) {
                // Không phải UUID hợp lệ, tiếp tục tìm theo email/sđt
            }
        }

        // 2. Tìm theo Email hoặc Số điện thoại
        if (recipientUser == null) {
            recipientUser = userRepository.findByIdentifier(query)
                    .orElseThrow(() -> new UserNotFoundException("Recipient not found with identifier: " + query));
        }

        // 3. Kiểm tra trạng thái tài khoản người nhận
        if (recipientUser.getStatus() != UserStatus.ACTIVE) {
            throw new UserNotActiveException("Recipient account is not active");
        }

        // 4. Kiểm tra ví người nhận nếu chưa tìm thấy ở bước 1
        if (recipientWallet == null) {
            recipientWallet = walletRepository.findByUserId(recipientUser.getId())
                    .orElseThrow(() -> new WalletNotFoundException("Recipient does not have an active wallet"));
        }

        if (recipientWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new com.tyson.digitalwallet.ddd.application.exception.AppException(
                    com.tyson.digitalwallet.ddd.application.exception.ErrorCode.WALLET_NOT_ACTIVE,
                    "Recipient wallet is currently not active"
            );
        }

        return new RecipientLookupResponse(
                recipientUser.getId(),
                recipientWallet.getId(),
                recipientUser.getFullName(),
                recipientUser.getPhoneNumber(),
                recipientUser.getEmail(),
                recipientWallet.getCurrency()
        );
    }
}
