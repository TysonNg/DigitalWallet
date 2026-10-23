package com.tyson.digitalwallet.ddd.infrastructure.persistence.adapter.payment;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tyson.digitalwallet.ddd.application.port.payment.*;
import com.tyson.digitalwallet.ddd.domain.model.entity.*;
import com.tyson.digitalwallet.ddd.infrastructure.adapter.payment.StripePayoutGateway;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class StripePayoutGatewayTest {
    private WithdrawalRequest request() {
        return WithdrawalRequest.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("200000"),
                new BigDecimal("1000000"),
                "VND",
                "test-bank:SUCCESS",
                "request-key-123");
    }

    @Test
    public void ambiguousTransferFollowedByAuthorizationFailureCannotRefund() {
        var stripe = mock(StripePayoutPort.class);
        var gateway = new StripePayoutGateway(stripe);
        var request = request();
        var persisted =
                new AtomicReference<>(
                        PayoutAttempt.create(request.getId(), "STRIPE", 800, "USD")
                                .checkpoint("account_reference", "acct_test"));
        when(stripe.transfer(any(), anyString(), anyLong()))
                .thenThrow(new StripePayoutPort.ProviderException("timeout", true, null));
        assertThrows(
                StripePayoutPort.ProviderException.class,
                () -> gateway.execute(request, persisted.get(), persisted::set));
        // Resume from persisted checkpoint, as a new worker after a restart would.
        doThrow(new StripePayoutPort.ProviderException("forbidden", false, null))
                .when(stripe)
                .transfer(any(), anyString(), anyLong());
        assertEquals(
                PayoutGateway.Outcome.REVIEW_REQUIRED,
                gateway.execute(request, persisted.get(), persisted::set).outcome());
    }

    @Test
    public void definitiveFirstPayoutRejectionCanCompensateKnownTransfer() {
        var stripe = mock(StripePayoutPort.class);
        var gateway = new StripePayoutGateway(stripe);
        var request = request();
        var persisted =
                new AtomicReference<>(
                        PayoutAttempt.create(request.getId(), "STRIPE", 800, "USD")
                                .checkpoint("account_reference", "acct_test")
                                .checkpoint("transfer_reference", "tr_test"));
        when(stripe.payout(any(), anyString(), anyLong()))
                .thenThrow(new StripePayoutPort.ProviderException("rejected", false, null));
        assertEquals(
                PayoutGateway.Outcome.FAILED,
                gateway.execute(request, persisted.get(), persisted::set).outcome());
        gateway.compensate(request, persisted.get());
        verify(stripe).reverseTransfer(request.getId(), "tr_test");
    }

    @Test
    public void checkpointedOperationsAreNotRecreatedAfterRestart() {
        var stripe = mock(StripePayoutPort.class);
        var gateway = new StripePayoutGateway(stripe);
        var request = request();
        var attempt =
                PayoutAttempt.create(request.getId(), "STRIPE", 800, "USD")
                        .checkpoint("account_reference", "acct_test")
                        .checkpoint("transfer_reference", "tr_test")
                        .checkpoint("payout_reference", "po_test");
        when(stripe.retrieve("acct_test", "po_test"))
                .thenReturn(
                        new StripePayoutPort.PayoutState(
                                "po_test", "acct_test", 800, "usd", "paid"));
        assertEquals(
                PayoutGateway.Outcome.PAID,
                gateway.execute(request, attempt, a -> fail("No new checkpoint expected"))
                        .outcome());
        verify(stripe, never()).transfer(any(), anyString(), anyLong());
        verify(stripe, never()).payout(any(), anyString(), anyLong());
    }
}
