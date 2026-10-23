package com.tyson.digitalwallet.ddd.domain.model.entity;

import static org.junit.Assert.*;

import com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.UUID;

public class WithdrawalLifecycleTest {
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
    public void completionCannotBeReleasedAsAnOrdinaryFailure() {
        var r = request();
        r.markProcessing();
        r.complete(UUID.randomUUID());
        assertThrows(IllegalStateException.class, () -> r.fail("rejected"));
        r.markReturned("bank returned funds");
        assertEquals(WithdrawalStatus.RETURNED, r.getStatus());
    }

    @Test
    public void failedRequestCannotBeCompleted() {
        var r = request();
        r.fail("rejected");
        assertThrows(IllegalStateException.class, () -> r.complete(UUID.randomUUID()));
    }

    @Test
    public void reviewDoesNotMakeHeldFundsAvailable() {
        var r = request();
        r.requireReview("unknown outcome");
        assertTrue(r.isHeld());
        assertEquals(WithdrawalStatus.REVIEW_REQUIRED, r.getStatus());
    }

    @Test
    public void providerCheckpointSurvivesRehydrationAndKeepsStableOperationId() {
        var attempt = PayoutAttempt.create(UUID.randomUUID(), "STRIPE", 800, "USD");
        var checkpoint = attempt.checkpoint("account_reference", "acct_test");
        assertEquals(attempt.getId(), checkpoint.getId());
        assertEquals("acct_test", checkpoint.getMetadata().get("account_reference"));
        assertFalse(attempt.getMetadata().containsKey("account_reference"));
    }
}
