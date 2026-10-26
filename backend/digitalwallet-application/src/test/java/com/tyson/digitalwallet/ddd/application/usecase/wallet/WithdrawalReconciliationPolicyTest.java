package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.tyson.digitalwallet.ddd.application.port.payment.PayoutNotificationInbox;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.impl.ReconcileWithdrawalUseCaseImpl;
import com.tyson.digitalwallet.ddd.domain.repository.WithdrawalRepository;

import org.junit.Test;

import java.util.*;

public class WithdrawalReconciliationPolicyTest {
    @Test
    public void kafkaModeUsesOnlyRecoveryCandidates() {
        var repository = mock(WithdrawalRepository.class);
        var processor = mock(ProcessWithdrawalUseCase.class);
        var id = UUID.randomUUID();
        when(repository.findRecoveryIds()).thenReturn(List.of(id));
        new ReconcileWithdrawalUseCaseImpl(
                        repository,
                        mock(PayoutNotificationInbox.class),
                        processor,
                        new WithdrawalProcessingPolicy("kafka"))
                .reconcile();
        verify(processor).process(id);
        verify(repository, never()).findPendingIds();
    }

    @Test
    public void schedulerModeKeepsExistingStartBehavior() {
        var repository = mock(WithdrawalRepository.class);
        var processor = mock(ProcessWithdrawalUseCase.class);
        var id = UUID.randomUUID();
        when(repository.findPendingIds()).thenReturn(List.of(id));
        new ReconcileWithdrawalUseCaseImpl(
                        repository,
                        mock(PayoutNotificationInbox.class),
                        processor,
                        new WithdrawalProcessingPolicy("scheduler"))
                .reconcile();
        verify(processor).process(id);
        verify(repository, never()).findRecoveryIds();
    }

    @Test
    public void invalidModeFailsFast() {
        assertThrows(
                IllegalArgumentException.class, () -> new WithdrawalProcessingPolicy("unknown"));
    }
}
