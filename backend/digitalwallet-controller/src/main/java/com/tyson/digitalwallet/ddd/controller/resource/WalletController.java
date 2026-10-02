package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.CreateWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.DepositMoneyUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.GetWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.WithdrawUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.wallet.CreateWalletRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.wallet.DepositMoneyRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.wallet.WithdrawRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.wallet.WalletResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final CreateWalletUseCase createWalletUseCase;
    private final DepositMoneyUseCase depositMoneyUseCase;
    private final WithdrawUseCase withdrawUseCase;
    private final GetWalletUseCase getWalletUseCase;

    public WalletController(
            CreateWalletUseCase createWalletUseCase,
            DepositMoneyUseCase depositMoneyUseCase,
            WithdrawUseCase withdrawUseCase,
            GetWalletUseCase getWalletUseCase
    ) {
        this.createWalletUseCase = createWalletUseCase;
        this.depositMoneyUseCase = depositMoneyUseCase;
        this.withdrawUseCase = withdrawUseCase;
        this.getWalletUseCase = getWalletUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WalletResponseDto>> create(@RequestBody @Valid CreateWalletRequestDto createWalletRequestDto) {
        WalletResponse response = createWalletUseCase.createWallet(createWalletRequestDto.toCommand());
        return ApiResponse.created("Wallet created successfully", WalletResponseDto.from(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WalletResponseDto>> getWalletById(@PathVariable UUID id) {
        WalletResponse response = getWalletUseCase.getWalletById(id);
        return ApiResponse.ok("Get wallet successfully", WalletResponseDto.from(response));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<WalletResponseDto>> getWalletByUserId(@PathVariable UUID userId) {
        WalletResponse response = getWalletUseCase.getWalletByUserId(userId);
        return ApiResponse.ok("Get wallet successfully", WalletResponseDto.from(response));
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<WalletResponseDto>> depositMoney(@RequestBody @Valid DepositMoneyRequestDto depositMoneyRequestDto) {
        WalletResponse response = depositMoneyUseCase.deposit(depositMoneyRequestDto.toCommand());
        return ApiResponse.ok("Deposited successfully", WalletResponseDto.from(response));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WalletResponseDto>> withdraw(@RequestBody @Valid WithdrawRequestDto withdrawRequestDto) {
        WalletResponse response = withdrawUseCase.withdraw(withdrawRequestDto.toCommand());
        return ApiResponse.ok("Withdraw successfully", WalletResponseDto.from(response));
    }
}
