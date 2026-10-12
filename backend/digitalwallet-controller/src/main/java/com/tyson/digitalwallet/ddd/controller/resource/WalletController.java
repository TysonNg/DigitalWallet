package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.transaction.GetTransactionsUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.CreateWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.GetWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.TransferMoneyUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.WithdrawUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.CreateWalletCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.wallet.CreateWalletRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.wallet.TransferMoneyRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.wallet.WithdrawRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.transaction.TransactionResponseDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.wallet.WalletResponseDto;
import com.tyson.digitalwallet.ddd.controller.security.WalletAccess;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/wallet")
public class WalletController {
    private final CreateWalletUseCase createWalletUseCase;
    private final WithdrawUseCase withdrawUseCase;
    private final GetWalletUseCase getWalletUseCase;
    private final TransferMoneyUseCase transferMoneyUseCase;
    private final GetTransactionsUseCase getTransactionsUseCase;

    public WalletController(CreateWalletUseCase createWalletUseCase, WithdrawUseCase withdrawUseCase,
                            GetWalletUseCase getWalletUseCase, TransferMoneyUseCase transferMoneyUseCase,
                            GetTransactionsUseCase getTransactionsUseCase) {
        this.createWalletUseCase = createWalletUseCase;
        this.withdrawUseCase = withdrawUseCase;
        this.getWalletUseCase = getWalletUseCase;
        this.transferMoneyUseCase = transferMoneyUseCase;
        this.getTransactionsUseCase = getTransactionsUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WalletResponseDto>> create(
            @RequestBody @Valid CreateWalletRequestDto request, Principal principal) {
        UUID userId = WalletAccess.currentUser(principal);
        WalletAccess.requireUser(principal, request.userId());
        WalletResponse response = createWalletUseCase.createWallet(new CreateWalletCommand(userId, request.currency()));
        return ApiResponse.created("Wallet created successfully", WalletResponseDto.from(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WalletResponseDto>> getWalletById(@PathVariable("id") UUID id, Principal principal) {
        WalletResponse response = getWalletUseCase.getWalletById(id);
        WalletAccess.requireOwner(principal, response);
        return ApiResponse.ok("Get wallet successfully", WalletResponseDto.from(response));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<WalletResponseDto>> getWalletByUserId(
            @PathVariable("userId") UUID userId, Principal principal) {
        WalletAccess.requireUser(principal, userId);
        WalletResponse response = getWalletUseCase.getWalletByUserId(WalletAccess.currentUser(principal));
        return ApiResponse.ok("Get wallet successfully", WalletResponseDto.from(response));
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<WalletResponseDto>> depositMoney() {
        throw new SecurityException("Direct deposits are disabled. Use a payment gateway to fund your wallet.");
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WalletResponseDto>> withdraw(
            @RequestBody @Valid WithdrawRequestDto request, Principal principal) {
        WalletAccess.requireOwner(principal, request.walletId(), getWalletUseCase);
        WalletResponse response = withdrawUseCase.withdraw(request.toCommand());
        return ApiResponse.ok("Withdraw successfully", WalletResponseDto.from(response));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<WalletResponseDto>> transfer(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @RequestBody @Valid TransferMoneyRequestDto request, Principal principal) {
        WalletAccess.requireOwner(principal, request.senderWalletId(), getWalletUseCase);
        String idempotencyKey = (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank())
                ? idempotencyKeyHeader : request.idempotencyKey();
        var command = new com.tyson.digitalwallet.ddd.application.usecase.wallet.command.TransferMoneyCommand(
                request.senderWalletId(), request.receiverWalletId(), request.amount(), request.description(), idempotencyKey);
        WalletResponse response = transferMoneyUseCase.transfer(command);
        return ApiResponse.ok("Transferred successfully", WalletResponseDto.from(response));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponseDto>>> getWalletTransactions(
            @PathVariable("id") UUID id, Principal principal) {
        WalletAccess.requireOwner(principal, id, getWalletUseCase);
        List<TransactionResponseDto> list = getTransactionsUseCase.getTransactionsByWalletId(id).stream()
                .map(TransactionResponseDto::from).toList();
        return ApiResponse.ok("Get transactions successfully", list);
    }
}