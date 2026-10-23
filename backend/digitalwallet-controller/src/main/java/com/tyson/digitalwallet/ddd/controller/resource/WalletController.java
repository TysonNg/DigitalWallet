package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.transaction.GetTransactionsUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.RecipientLookupResponse;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.wallet.*;
import com.tyson.digitalwallet.ddd.controller.dto.response.transaction.TransactionResponseDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.wallet.RecipientLookupResponseDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.wallet.WalletResponseDto;
import com.tyson.digitalwallet.ddd.controller.ratelimit.RateLimit;
import com.tyson.digitalwallet.ddd.controller.ratelimit.RateLimitType;
import com.tyson.digitalwallet.ddd.controller.security.WalletAccess;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final LookupRecipientUseCase lookupRecipientUseCase;
    private final SetupWalletPinUseCase setupWalletPinUseCase;
    private final ChangeWalletPinUseCase changeWalletPinUseCase;

    public WalletController(CreateWalletUseCase createWalletUseCase, WithdrawUseCase withdrawUseCase,
                            GetWalletUseCase getWalletUseCase, TransferMoneyUseCase transferMoneyUseCase,
                            GetTransactionsUseCase getTransactionsUseCase,
                            LookupRecipientUseCase lookupRecipientUseCase) {
        this(createWalletUseCase, withdrawUseCase, getWalletUseCase, transferMoneyUseCase,
                getTransactionsUseCase, lookupRecipientUseCase, null, null);
    }

    @Autowired
    public WalletController(CreateWalletUseCase createWalletUseCase, WithdrawUseCase withdrawUseCase,
                            GetWalletUseCase getWalletUseCase, TransferMoneyUseCase transferMoneyUseCase,
                            GetTransactionsUseCase getTransactionsUseCase,
                            LookupRecipientUseCase lookupRecipientUseCase,
                            SetupWalletPinUseCase setupWalletPinUseCase,
                            ChangeWalletPinUseCase changeWalletPinUseCase) {
        this.createWalletUseCase = createWalletUseCase;
        this.withdrawUseCase = withdrawUseCase;
        this.getWalletUseCase = getWalletUseCase;
        this.transferMoneyUseCase = transferMoneyUseCase;
        this.getTransactionsUseCase = getTransactionsUseCase;
        this.lookupRecipientUseCase = lookupRecipientUseCase;
        this.setupWalletPinUseCase = setupWalletPinUseCase;
        this.changeWalletPinUseCase = changeWalletPinUseCase;
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

    @RateLimit(name = "transferRateLimiter", type = RateLimitType.USER_OR_IP)
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WalletResponseDto>> withdraw(
            @RequestBody @Valid WithdrawRequestDto request, Principal principal) {
        WalletAccess.requireOwner(principal, request.walletId(), getWalletUseCase);
        throw new SecurityException("Legacy local withdrawals are disabled. Use /wallet/withdrawals for Stripe test payouts.");
    }

    @RateLimit(name = "transferRateLimiter", type = RateLimitType.USER_OR_IP)
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<WalletResponseDto>> transfer(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @RequestBody @Valid TransferMoneyRequestDto request, Principal principal) {
        WalletAccess.requireOwner(principal, request.senderWalletId(), getWalletUseCase);
        String idempotencyKey = (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank())
                ? idempotencyKeyHeader : request.idempotencyKey();
        var command = new TransferMoneyCommand(
                request.senderWalletId(), request.receiverWalletId(), request.amount(), request.description(), idempotencyKey, request.pin());
        WalletResponse response = transferMoneyUseCase.transfer(command);
        return ApiResponse.ok("Transferred successfully", WalletResponseDto.from(response));
    }

    @PostMapping("/pin/setup")
    public ResponseEntity<ApiResponse<Void>> setupPin(
            @RequestBody @Valid SetupPinRequestDto request,
            Principal principal) {
        UUID userId = WalletAccess.currentUser(principal);
        UUID walletId = request.walletId();
        if (walletId != null) {
            WalletAccess.requireOwner(principal, walletId, getWalletUseCase);
        }
        setupWalletPinUseCase.setupPin(new SetupWalletPinCommand(
                userId, walletId, request.pin()
        ));
        return ApiResponse.ok("Thiết lập mã PIN thành công", null);
    }

    @PostMapping("/pin/change")
    public ResponseEntity<ApiResponse<Void>> changePin(
            @RequestBody @Valid ChangePinRequestDto request,
            Principal principal) {
        UUID userId = WalletAccess.currentUser(principal);
        UUID walletId = request.walletId();
        if (walletId != null) {
            WalletAccess.requireOwner(principal, walletId, getWalletUseCase);
        }
        changeWalletPinUseCase.changePin(new ChangeWalletPinCommand(
                userId, walletId, request.oldPin(), request.newPin()
        ));
        return ApiResponse.ok("Đổi mã PIN thành công", null);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponseDto>>> getWalletTransactions(
            @PathVariable("id") UUID id, Principal principal) {
        WalletAccess.requireOwner(principal, id, getWalletUseCase);
        List<TransactionResponseDto> list = getTransactionsUseCase.getTransactionsByWalletId(id).stream()
                .map(TransactionResponseDto::from).toList();
        return ApiResponse.ok("Get transactions successfully", list);
    }

    @RateLimit(name = "lookupRateLimiter", type = RateLimitType.USER_OR_IP)
    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<RecipientLookupResponseDto>> lookupRecipient(
            @RequestParam("identifier") String identifier,
            Principal principal
    ) {
        UUID currentUserId = WalletAccess.currentUser(principal);
        RecipientLookupResponse response =
                lookupRecipientUseCase.lookup(currentUserId, identifier);
        return ApiResponse.ok("Tra cứu người nhận thành công",
                RecipientLookupResponseDto.from(response));
    }
}
