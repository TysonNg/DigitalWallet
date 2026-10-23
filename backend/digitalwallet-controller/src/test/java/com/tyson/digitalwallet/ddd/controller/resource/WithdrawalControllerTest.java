package com.tyson.digitalwallet.ddd.controller.resource;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.CreateWithdrawalCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WithdrawalResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.wallet.CreateWithdrawalRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.wallet.WithdrawalResponseDto;
import com.tyson.digitalwallet.ddd.domain.model.entity.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.*;

public class WithdrawalControllerTest {
    @Test
    public void legacySandboxRequestMapsToCommonCommandAndAuthenticatedOwner() {
        UUID user = UUID.randomUUID(), wallet = UUID.randomUUID();
        var create = mock(CreateWithdrawalUseCase.class);
        var r =
                WithdrawalRequest.create(
                        wallet,
                        user,
                        new BigDecimal("200000"),
                        new BigDecimal("1000000"),
                        "VND",
                        "test-bank:SUCCESS",
                        "request-key-123");
        when(create.create(any()))
                .thenReturn(
                        WithdrawalResponse.from(
                                r, PayoutAttempt.create(r.getId(), "STRIPE", 800, "USD")));
        var controller =
                new WithdrawalController(
                        create,
                        mock(GetWithdrawalHistoryUseCase.class),
                        mock(HandlePayoutNotificationUseCase.class));
        controller.create(
                new CreateWithdrawalRequestDto(
                        wallet, r.getAmount(), r.getIdempotencyKey(), null, null, "SUCCESS", null),
                () -> user.toString());
        var command = ArgumentCaptor.forClass(CreateWithdrawalCommand.class);
        verify(create).create(command.capture());
        assertEquals(user, command.getValue().userId());
        assertEquals("STRIPE", command.getValue().provider());
        assertEquals("test-bank:SUCCESS", command.getValue().destination());
    }

    @Test
    public void payoutAmountUsesProviderCurrencyExponent() {
        var r =
                WithdrawalRequest.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("200000"),
                        new BigDecimal("1000000"),
                        "VND",
                        "bank-token",
                        "request-key-123");
        var attempt =
                PayoutAttempt.create(r.getId(), "EXAMPLE", 200000, "VND")
                        .checkpoint("currency_exponent", "0");
        var dto = WithdrawalResponseDto.from(WithdrawalResponse.from(r, attempt));
        assertEquals(new BigDecimal("200000"), dto.payoutAmount());
        assertEquals("EXAMPLE", dto.provider());
        assertNull(dto.stripeAccountId());
    }
}
