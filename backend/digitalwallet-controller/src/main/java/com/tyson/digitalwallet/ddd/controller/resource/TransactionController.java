package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.transaction.GetTransactionsUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.transaction.response.TransactionResponse;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.GetWalletUseCase;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.response.transaction.TransactionResponseDto;
import com.tyson.digitalwallet.ddd.controller.security.WalletAccess;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final GetTransactionsUseCase getTransactionsUseCase;
    private final GetWalletUseCase getWalletUseCase;

    public TransactionController(GetTransactionsUseCase getTransactionsUseCase, GetWalletUseCase getWalletUseCase) {
        this.getTransactionsUseCase = getTransactionsUseCase;
        this.getWalletUseCase = getWalletUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> getTransactionById(
            @PathVariable("id") UUID id, Principal principal) {
        UUID userId = WalletAccess.currentUser(principal);
        TransactionResponse response = getTransactionsUseCase.getTransactionById(id);
        if (!WalletAccess.owns(userId, response.senderWalletId(), getWalletUseCase)
                && !WalletAccess.owns(userId, response.receiverWalletId(), getWalletUseCase)) {
            throw new SecurityException("You cannot access another user's transaction");
        }
        return ApiResponse.ok("Get transaction successfully", TransactionResponseDto.from(response));
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<ApiResponse<List<TransactionResponseDto>>> getTransactionsByWalletId(
            @PathVariable("walletId") UUID walletId, Principal principal) {
        WalletAccess.requireOwner(principal, walletId, getWalletUseCase);
        List<TransactionResponseDto> list = getTransactionsUseCase.getTransactionsByWalletId(walletId).stream()
                .map(TransactionResponseDto::from).toList();
        return ApiResponse.ok("Get transactions successfully", list);
    }
}