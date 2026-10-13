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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BeneficiaryUseCaseImpl implements BeneficiaryUseCase {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public BeneficiaryUseCaseImpl(
            BeneficiaryRepository beneficiaryRepository,
            UserRepository userRepository,
            WalletRepository walletRepository
    ) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getBeneficiaries(UUID userId) {
        return beneficiaryRepository.findAllByUserId(userId).stream()
                .map(BeneficiaryResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public BeneficiaryResponse addBeneficiary(AddBeneficiaryCommand command) {
        UUID userId = command.userId();
        UUID beneficiaryUserId = command.beneficiaryUserId();
        UUID beneficiaryWalletId = command.beneficiaryWalletId();

        if (userId == null || beneficiaryUserId == null || beneficiaryWalletId == null) {
            throw new IllegalArgumentException("Thông tin người thụ hưởng không đầy đủ");
        }

        if (userId.equals(beneficiaryUserId)) {
            throw new IllegalArgumentException("Không thể thêm chính mình vào danh bạ thụ hưởng");
        }

        if (beneficiaryRepository.existsByUserIdAndBeneficiaryUserId(userId, beneficiaryUserId)) {
            throw new IllegalStateException("Người nhận này đã có trong danh bạ thụ hưởng");
        }

        User targetUser = userRepository.findById(beneficiaryUserId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng thụ hưởng"));

        walletRepository.findById(beneficiaryWalletId)
                .orElseThrow(() -> new WalletNotFoundException("Ví thụ hưởng không tồn tại"));

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

        return BeneficiaryResponse.from(saved);
    }

    @Override
    @Transactional
    public BeneficiaryResponse updateBeneficiary(UpdateBeneficiaryCommand command) {
        Beneficiary beneficiary = beneficiaryRepository.findById(command.beneficiaryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy liên hệ thụ hưởng"));

        if (!beneficiary.getUserId().equals(command.userId())) {
            throw new SecurityException("Bạn không có quyền chỉnh sửa liên hệ thụ hưởng này");
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

        return BeneficiaryResponse.from(updated);
    }

    @Override
    @Transactional
    public void deleteBeneficiary(UUID beneficiaryId, UUID userId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy liên hệ thụ hưởng"));

        if (!beneficiary.getUserId().equals(userId)) {
            throw new SecurityException("Bạn không có quyền xoá liên hệ thụ hưởng này");
        }

        beneficiaryRepository.delete(beneficiary);
    }
}
