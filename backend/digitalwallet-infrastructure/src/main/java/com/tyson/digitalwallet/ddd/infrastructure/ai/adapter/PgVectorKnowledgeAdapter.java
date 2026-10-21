package com.tyson.digitalwallet.ddd.infrastructure.ai.adapter;

import com.tyson.digitalwallet.ddd.application.port.out.ai.VectorKnowledgePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PgVectorKnowledgeAdapter implements VectorKnowledgePort {

    private static final Logger log = LoggerFactory.getLogger(PgVectorKnowledgeAdapter.class);

    private final VectorStore vectorStore;

    public PgVectorKnowledgeAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<String> findRelevantPolicies(String query, int topK) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(Math.max(1, topK))
                    .build();

            List<Document> documents = vectorStore.similaritySearch(request);
            if (documents == null || documents.isEmpty()) {
                return Collections.emptyList();
            }

            return documents.stream()
                    .map(Document::getText)
                    .toList();
        } catch (Exception e) {
            log.warn("Similarity search in vector_store failed (falling back to empty list): {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
