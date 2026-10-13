package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.beneficiary.BeneficiaryUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.beneficiary.response.BeneficiaryResponse;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.beneficiary.AddBeneficiaryRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.beneficiary.UpdateBeneficiaryRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.beneficiary.BeneficiaryResponseDto;
import com.tyson.digitalwallet.ddd.controller.security.WalletAccess;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryUseCase beneficiaryUseCase;

    public BeneficiaryController(BeneficiaryUseCase beneficiaryUseCase) {
        this.beneficiaryUseCase = beneficiaryUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BeneficiaryResponseDto>>> getBeneficiaries(Principal principal) {
        UUID userId = WalletAccess.currentUser(principal);
        List<BeneficiaryResponseDto> list = beneficiaryUseCase.getBeneficiaries(userId).stream()
                .map(BeneficiaryResponseDto::from)
                .toList();
        return ApiResponse.ok("Lấy danh bạ thụ hưởng thành công", list);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BeneficiaryResponseDto>> addBeneficiary(
            @RequestBody @Valid AddBeneficiaryRequestDto request,
            Principal principal
    ) {
        UUID userId = WalletAccess.currentUser(principal);
        BeneficiaryResponse response = beneficiaryUseCase.addBeneficiary(request.toCommand(userId));
        return ApiResponse.created("Thêm người thụ hưởng thành công", BeneficiaryResponseDto.from(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BeneficiaryResponseDto>> updateBeneficiary(
            @PathVariable("id") UUID id,
            @RequestBody @Valid UpdateBeneficiaryRequestDto request,
            Principal principal
    ) {
        UUID userId = WalletAccess.currentUser(principal);
        BeneficiaryResponse response = beneficiaryUseCase.updateBeneficiary(request.toCommand(id, userId));
        return ApiResponse.ok("Cập nhật tên gợi nhớ thành công", BeneficiaryResponseDto.from(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBeneficiary(
            @PathVariable("id") UUID id,
            Principal principal
    ) {
        UUID userId = WalletAccess.currentUser(principal);
        beneficiaryUseCase.deleteBeneficiary(id, userId);
        return ApiResponse.ok("Xoá người thụ hưởng thành công", null);
    }
}
