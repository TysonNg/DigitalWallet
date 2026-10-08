package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.transaction.GetTransactionsUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.CreateWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.DepositMoneyUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.GetWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.TransferMoneyUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.WithdrawUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.wallet.CreateWalletRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.wallet.DepositMoneyRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.wallet.TransferMoneyRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.wallet.WithdrawRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.transaction.TransactionResponseDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.wallet.WalletResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final CreateWalletUseCase createWalletUseCase;
    private final DepositMoneyUseCase depositMoneyUseCase;
    private final WithdrawUseCase withdrawUseCase;
    private final GetWalletUseCase getWalletUseCase;
    private final TransferMoneyUseCase transferMoneyUseCase;
    private final GetTransactionsUseCase getTransactionsUseCase;

    public WalletController(
            CreateWalletUseCase createWalletUseCase,
            DepositMoneyUseCase depositMoneyUseCase,
            WithdrawUseCase withdrawUseCase,
            GetWalletUseCase getWalletUseCase,
            TransferMoneyUseCase transferMoneyUseCase,
            GetTransactionsUseCase getTransactionsUseCase
    ) {
        this.createWalletUseCase = createWalletUseCase;
        this.depositMoneyUseCase = depositMoneyUseCase;
        this.withdrawUseCase = withdrawUseCase;
        this.getWalletUseCase = getWalletUseCase;
        this.transferMoneyUseCase = transferMoneyUseCase;
        this.getTransactionsUseCase = getTransactionsUseCase;
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

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<WalletResponseDto>> transfer(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @RequestBody @Valid TransferMoneyRequestDto requestDto
    ) {
        // Ưu tiên Idempotency-Key từ HTTP Header, nếu không có thì lấy từ Request Body
        String idempotencyKey = (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank())
                ? idempotencyKeyHeader
                : requestDto.idempotencyKey();

        var command = new com.tyson.digitalwallet.ddd.application.usecase.wallet.command.TransferMoneyCommand(
                requestDto.senderWalletId(),
                requestDto.receiverWalletId(),
                requestDto.amount(),
                requestDto.description(),
                idempotencyKey
        );

        WalletResponse response = transferMoneyUseCase.transfer(command);
        return ApiResponse.ok("Transferred successfully", WalletResponseDto.from(response));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponseDto>>> getWalletTransactions(@PathVariable UUID id) {
        List<TransactionResponseDto> list = getTransactionsUseCase.getTransactionsByWalletId(id).stream()
                .map(TransactionResponseDto::from)
                .toList();
        return ApiResponse.ok("Get transactions successfully", list);
    }
}
