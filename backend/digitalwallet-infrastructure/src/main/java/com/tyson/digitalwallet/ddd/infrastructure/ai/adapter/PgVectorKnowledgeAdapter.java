package com.tyson.digitalwallet.ddd.infrastructure.ai.adapter;

import com.tyson.digitalwallet.ddd.application.port.out.ai.VectorKnowledgePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@Component
public class PgVectorKnowledgeAdapter implements VectorKnowledgePort {

    private static final Logger log = LoggerFactory.getLogger(PgVectorKnowledgeAdapter.class);

    private final VectorStore vectorStore;
    @Value("${wallet.ai.retrieval-threshold:0.65}")
    private double retrievalThreshold = 0.65;
    private final com.tyson.digitalwallet.ddd.infrastructure.ai.service.LocalKnowledgeBaseService localKnowledgeBaseService;

    public PgVectorKnowledgeAdapter(VectorStore vectorStore,
                                    com.tyson.digitalwallet.ddd.infrastructure.ai.service.LocalKnowledgeBaseService localKnowledgeBaseService) {
        this.vectorStore = vectorStore;
        this.localKnowledgeBaseService = localKnowledgeBaseService;
    }

    @Override
    public List<String> findRelevantPolicies(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) return List.of();
        // Fast-path: Nếu ở chế độ offline/demo, tra cứu tức thì từ Local Knowledge Base (< 1ms)
        // Tránh gọi OpenAI Embedding API gây timeout mạng OkHttp 60s
        if (localKnowledgeBaseService.isOfflineMode()) {
            return localKnowledgeBaseService.searchPolicies(query, topK);
        }

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(Math.max(1, topK))
                    .similarityThreshold(retrievalThreshold)
                    .filterExpression("type == 'financial_policy'")
                    .build();

            List<Document> documents = vectorStore.similaritySearch(request);
            if (documents != null && !documents.isEmpty()) {
                return documents.stream()
                        .map(document -> "[Nguồn: " + document.getMetadata().getOrDefault("source", "unknown")
                                + ", chunk: " + document.getMetadata().getOrDefault("chunk", "unknown") + "]\n" + document.getText())
                        .distinct()
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Similarity search in vector_store failed (falling back to local knowledge base): {}", e.getMessage());
        }

        return localKnowledgeBaseService.searchPolicies(query, topK);
    }
}
