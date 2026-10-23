package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.CreateWithdrawalCommand;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.wallet.CreateWithdrawalRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.wallet.WithdrawalResponseDto;
import com.tyson.digitalwallet.ddd.controller.ratelimit.*;
import com.tyson.digitalwallet.ddd.controller.security.WalletAccess;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@RestController
public class WithdrawalController {
    private final CreateWithdrawalUseCase create;
    private final GetWithdrawalHistoryUseCase history;
    private final HandlePayoutNotificationUseCase notifications;

    public WithdrawalController(
            CreateWithdrawalUseCase create,
            GetWithdrawalHistoryUseCase history,
            HandlePayoutNotificationUseCase notifications) {
        this.create = create;
        this.history = history;
        this.notifications = notifications;
    }

    @RateLimit(name = "transferRateLimiter", type = RateLimitType.USER_OR_IP)
    @PostMapping("/wallet/withdrawals")
    public ResponseEntity<ApiResponse<WithdrawalResponseDto>> create(
            @RequestBody @Valid CreateWithdrawalRequestDto request, Principal principal) {
        String provider = request.provider() == null ? "STRIPE" : request.provider();
        String destination = request.destination();
        if (destination == null && "STRIPE".equals(provider) && request.testBank() != null)
            destination = "test-bank:" + request.testBank();
        var command =
                new CreateWithdrawalCommand(
                        WalletAccess.currentUser(principal),
                        request.walletId(),
                        request.amount(),
                        request.idempotencyKey(),
                        provider,
                        destination,
                        request.pin());
        return ApiResponse.ok(
                "Withdrawal reserved", WithdrawalResponseDto.from(create.create(command)));
    }

    @GetMapping("/wallet/withdrawals")
    public ResponseEntity<ApiResponse<List<WithdrawalResponseDto>>> history(
            @RequestParam UUID walletId, Principal principal) {
        return ApiResponse.ok(
                "Withdrawal history",
                history.history(WalletAccess.currentUser(principal), walletId).stream()
                        .map(WithdrawalResponseDto::from)
                        .toList());
    }

    @PostMapping("/payments/webhook/stripe-connect")
    public ResponseEntity<Map<String, Boolean>> webhook(
            @RequestBody String body,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        notifications.webhook("STRIPE", body, signature);
        return ResponseEntity.ok(Map.of("received", true));
    }
}
