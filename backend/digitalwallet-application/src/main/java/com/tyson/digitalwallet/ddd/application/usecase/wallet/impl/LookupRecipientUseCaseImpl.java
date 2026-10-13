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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class LookupRecipientUseCaseImpl implements LookupRecipientUseCase {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public LookupRecipientUseCaseImpl(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RecipientLookupResponse lookup(UUID currentUserId, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Thông tin người nhận (SĐT/Email/Mã ví) không được để trống");
        }

        String query = identifier.trim();
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
                    .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người nhận với thông tin: " + query));
        }

        // 3. Kiểm tra không được chuyển cho chính mình
        if (currentUserId != null && recipientUser.getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Không thể chuyển tiền cho chính tài khoản của bạn");
        }

        // 4. Kiểm tra trạng thái tài khoản người nhận
        if (recipientUser.getStatus() != UserStatus.ACTIVE) {
            throw new UserNotActiveException("Tài khoản người nhận hiện không trong trạng thái hoạt động");
        }

        // 5. Kiểm tra ví người nhận nếu chưa tìm thấy ở bước 1
        if (recipientWallet == null) {
            recipientWallet = walletRepository.findByUserId(recipientUser.getId())
                    .orElseThrow(() -> new WalletNotFoundException("Người nhận chưa mở ví hoặc ví không tồn tại"));
        }

        if (recipientWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Ví của người nhận hiện đang bị tạm khóa hoặc không hoạt động");
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
