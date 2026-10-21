package com.tyson.digitalwallet.ddd.infrastructure.ai;

import com.tyson.digitalwallet.ddd.infrastructure.ai.service.LocalKnowledgeBaseService;
import com.tyson.digitalwallet.ddd.infrastructure.ai.tools.FinancialAgentTools;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class LocalKnowledgeBaseServiceTest {
    @Test
    public void policyResponse_usesRetrievedDocumentInsteadOfHardcodedFee() {
        var service = new LocalKnowledgeBaseService(mock(FinancialAgentTools.class));
        String answer = service.generateSmartResponse("Phí Stripe?", "An", null,
                List.of("[Nguồn: fee-policy.md] Phí Stripe là 7.25%."));
        assertTrue(answer, answer.contains("7.25%"));
        assertFalse(answer, answer.contains("2.5%"));
    }

    @Test
    public void groupedMoney_isParsedAsFiftyThousand() {
        var tools = mock(FinancialAgentTools.class);
        when(tools.prepareDraftTransfer(anyString(), any(java.math.BigDecimal.class), anyString())).thenReturn("draft");
        var service = new LocalKnowledgeBaseService(tools);
        service.generateSmartResponse("Chuyển 50.000đ cho Nam nội dung ăn trưa", "An", null, List.of());
        verify(tools).prepareDraftTransfer(eq("Nam"), argThat(amount -> amount.compareTo(java.math.BigDecimal.valueOf(50000)) == 0), eq("ăn trưa"));
    }

    @Test
    public void guiCommand_reachesTransferParser() {
        var tools = mock(FinancialAgentTools.class);
        when(tools.prepareDraftTransfer(anyString(), any(java.math.BigDecimal.class), anyString())).thenReturn("draft");
        new LocalKnowledgeBaseService(tools).generateSmartResponse("Gửi 50k cho Nam nội dung ăn trưa", "An", null, List.of());
        verify(tools).prepareDraftTransfer(eq("Nam"), argThat(amount -> amount.compareTo(java.math.BigDecimal.valueOf(50000)) == 0), eq("ăn trưa"));
    }

    @Test
    public void unrelatedQuestion_doesNotReturnPolicies() {
        var service = new LocalKnowledgeBaseService(mock(FinancialAgentTools.class));
        service.init();
        assertTrue(service.searchPolicies("thời tiết hôm nay thế nào", 3).isEmpty());
    }

    @Test
    public void matchingPolicy_keepsSourceAndZeroTopKReturnsNothing() {
        var service = new LocalKnowledgeBaseService(mock(FinancialAgentTools.class));
        service.init();
        var policies = service.searchPolicies("Phí nạp Stripe", 3);
        assertFalse(policies.isEmpty());
        assertTrue(policies.getFirst(), policies.getFirst().contains("fee-policy.md"));
        assertTrue(service.searchPolicies("Stripe", 0).isEmpty());
    }

    @Test
    public void ambiguousTransferAsksForClarificationWithoutCallingTool() {
        var tools = mock(FinancialAgentTools.class);
        var service = new LocalKnowledgeBaseService(tools);
        String answer = service.generateSmartResponse("Chuyển 1,5 cho Nam", "An", null, List.of("fee policy"));
        assertTrue(answer, answer.contains("Vui lòng ghi rõ"));
        verifyNoInteractions(tools);
    }
}
