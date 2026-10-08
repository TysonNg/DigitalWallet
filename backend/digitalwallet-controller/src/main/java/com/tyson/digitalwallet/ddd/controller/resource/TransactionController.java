package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.transaction.GetTransactionsUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.transaction.response.TransactionResponse;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.response.transaction.TransactionResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final GetTransactionsUseCase getTransactionsUseCase;

    public TransactionController(GetTransactionsUseCase getTransactionsUseCase) {
        this.getTransactionsUseCase = getTransactionsUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> getTransactionById(@PathVariable UUID id) {
        TransactionResponse response = getTransactionsUseCase.getTransactionById(id);
        return ApiResponse.ok("Get transaction successfully", TransactionResponseDto.from(response));
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<ApiResponse<List<TransactionResponseDto>>> getTransactionsByWalletId(@PathVariable UUID walletId) {
        List<TransactionResponseDto> list = getTransactionsUseCase.getTransactionsByWalletId(walletId).stream()
                .map(TransactionResponseDto::from)
                .toList();
        return ApiResponse.ok("Get transactions successfully", list);
    }
}
