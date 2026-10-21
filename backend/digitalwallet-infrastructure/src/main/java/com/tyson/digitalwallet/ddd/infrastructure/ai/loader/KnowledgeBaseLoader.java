package com.tyson.digitalwallet.ddd.infrastructure.ai.loader;

import com.tyson.digitalwallet.ddd.application.usecase.ai.SyncKnowledgeUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class KnowledgeBaseLoader implements ApplicationRunner, SyncKnowledgeUseCase {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseLoader.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public KnowledgeBaseLoader(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int sync() {
        return ingestKnowledgeBase();
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Kiểm tra xem bảng vector_store đã có dữ liệu hay chưa
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vector_store", Integer.class
            );

            if (count != null && count > 0) {
                log.info("Knowledge Base already contains {} chunks in vector_store. Skipping initial ingestion.", count);
                return;
            }

            log.info("Starting automatic Knowledge Base ingestion for Digital Wallet...");
            ingestKnowledgeBase();
        } catch (Exception e) {
            log.warn("Knowledge Base auto-ingestion skipped or deferred (Vector DB or API not ready yet): {}", e.getMessage());
        }
    }

    public synchronized int ingestKnowledgeBase() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:knowledge-base/*.md");

            if (resources.length == 0) {
                log.warn("No knowledge-base markdown files found under classpath:knowledge-base/*.md");
                return 0;
            }

            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(450)
                    .withMinChunkSizeChars(100)
                    .withKeepSeparator(true)
                    .build();

            List<Document> allChunks = new ArrayList<>();

            for (Resource resource : resources) {
                String filename = resource.getFilename() != null ? resource.getFilename() : "unknown.md";
                try (InputStream is = resource.getInputStream()) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Document doc = new Document(content, Map.of(
                            "source", filename,
                            "type", "financial_policy",
                            "ingested_at", Instant.now().toString()
                    ));
                    List<Document> chunks = splitter.apply(List.of(doc));
                    allChunks.addAll(chunks);
                    log.info("Read {} -> split into {} chunks", filename, chunks.size());
                }
            }

            if (!allChunks.isEmpty()) {
                vectorStore.add(allChunks);
                log.info("Successfully ingested total {} chunks into PostgreSQL vector_store", allChunks.size());
            }

            return allChunks.size();
        } catch (Exception e) {
            log.error("Failed to ingest knowledge base: {}", e.getMessage(), e);
            throw new RuntimeException("Knowledge ingestion failed: " + e.getMessage(), e);
        }
    }
}
