package com.tyson.digitalwallet.ddd.infrastructure.ai;

import com.tyson.digitalwallet.ddd.infrastructure.ai.adapter.PgVectorKnowledgeAdapter;
import com.tyson.digitalwallet.ddd.infrastructure.ai.service.LocalKnowledgeBaseService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.*;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

public class PgVectorKnowledgeAdapterTest {
    @Test
    public void retrievalFiltersPoliciesAndReturnsSources() {
        VectorStore store = mock(VectorStore.class);
        var local = mock(LocalKnowledgeBaseService.class);
        when(store.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("Policy", Map.of("source", "fees.md", "chunk", 2))));
        var adapter = new PgVectorKnowledgeAdapter(store, local);
        var result = adapter.findRelevantPolicies("fee", 3);
        assertEquals(1, result.size());
        assertTrue(result.getFirst().contains("fees.md"));
        assertTrue(result.getFirst().contains("Policy"));
        var request = ArgumentCaptor.forClass(SearchRequest.class);
        verify(store).similaritySearch(request.capture());
        assertEquals(3, request.getValue().getTopK());
        assertEquals(0.65, request.getValue().getSimilarityThreshold(), 0.001);
        assertTrue(request.getValue().getFilterExpression().toString().contains("financial_policy"));
    }

    @Test
    public void failedVectorSearchFallsBackToLocalKnowledge() {
        VectorStore store = mock(VectorStore.class);
        var local = mock(LocalKnowledgeBaseService.class);
        when(store.similaritySearch(any(SearchRequest.class))).thenThrow(new IllegalStateException("offline"));
        when(local.searchPolicies("Stripe", 3)).thenReturn(List.of("local source"));
        assertEquals(List.of("local source"), new PgVectorKnowledgeAdapter(store, local).findRelevantPolicies("Stripe", 3));
    }
}
