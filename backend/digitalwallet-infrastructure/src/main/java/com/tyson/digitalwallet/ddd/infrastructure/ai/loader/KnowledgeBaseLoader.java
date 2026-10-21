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
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

@Component
public class KnowledgeBaseLoader implements ApplicationRunner, SyncKnowledgeUseCase {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseLoader.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final com.tyson.digitalwallet.ddd.infrastructure.ai.service.LocalKnowledgeBaseService localKnowledgeBaseService;
    private final ResourcePatternResolver resourceResolver;
    private final TransactionTemplate transactions;

    @Autowired
    public KnowledgeBaseLoader(VectorStore vectorStore,
                               JdbcTemplate jdbcTemplate,
                               com.tyson.digitalwallet.ddd.infrastructure.ai.service.LocalKnowledgeBaseService localKnowledgeBaseService) {
        this(vectorStore, jdbcTemplate, localKnowledgeBaseService, new PathMatchingResourcePatternResolver());
    }

    KnowledgeBaseLoader(VectorStore vectorStore,
                        JdbcTemplate jdbcTemplate,
                        com.tyson.digitalwallet.ddd.infrastructure.ai.service.LocalKnowledgeBaseService localKnowledgeBaseService,
                        ResourcePatternResolver resourceResolver) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.localKnowledgeBaseService = localKnowledgeBaseService;
        this.resourceResolver = resourceResolver;
        // PgVectorStore uses this same DataSource via JdbcTemplate. A programmatic
        // transaction also covers startup and sync's direct self-invocation.
        this.transactions = new TransactionTemplate(new DataSourceTransactionManager(
                Objects.requireNonNull(jdbcTemplate.getDataSource(), "Knowledge ingestion requires a DataSource")));
    }

    @Override
    public int sync() {
        return ingestKnowledgeBase();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (localKnowledgeBaseService.isOfflineMode()) {
            log.info("System is running in demo/offline mode (OPENAI_API_KEY is not set or is demo-key). Remote vector embedding ingestion skipped; Local Knowledge Base is active.");
            return;
        }

        try {
            // Kiểm tra xem bảng vector_store đã có dữ liệu hay chưa
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vector_store WHERE metadata->>'type' = 'financial_policy'", Integer.class
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
        if (localKnowledgeBaseService.isOfflineMode()) {
            throw new IllegalStateException("Knowledge sync is unavailable in offline mode: configure a real OpenAI API key before embedding.");
        }
        try {
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(450)
                    .withMinChunkSizeChars(100)
                    .withKeepSeparator(true)
                    .build();

            return transactions.execute(status -> {
                // Transaction-scoped lock serializes replacement across application instances.
                // Read resources after acquiring it so an older snapshot cannot overwrite a newer sync.
                jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
                    if ("PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
                        try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                            statement.setLong(1, 0x46494E504F4C4943L);
                            statement.execute();
                        }
                    }
                    return null;
                });
                List<Document> allChunks = readChunks(splitter);
                vectorStore.delete("type == 'financial_policy'");
                if (!allChunks.isEmpty()) {
                    vectorStore.add(allChunks);
                }
                log.info("Replaced financial policy knowledge with {} chunks", allChunks.size());
                return allChunks.size();
            });
        } catch (Exception e) {
            log.error("Failed to ingest knowledge base: {}", e.getMessage(), e);
            throw new RuntimeException("Knowledge ingestion failed: " + e.getMessage(), e);
        }
    }

    private List<Document> readChunks(TokenTextSplitter splitter) {
        try {
            Map<String, String> sources = new TreeMap<>();
            for (Resource resource : resourceResolver.getResources("classpath*:knowledge-base/*.md")) {
                String filename = Objects.requireNonNull(resource.getFilename(), "Knowledge resource requires a filename");
                try (InputStream input = resource.getInputStream()) {
                    String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    String previous = sources.putIfAbsent(filename, content);
                    if (previous != null && !previous.equals(content)) {
                        throw new IllegalStateException("Conflicting knowledge resources named " + filename);
                    }
                }
            }
            List<Document> allChunks = new ArrayList<>();
            for (var source : sources.entrySet()) {
                if (source.getValue().isBlank()) {
                    continue;
                }
                List<Document> chunks = splitter.apply(List.of(new Document(source.getValue())));
                for (int index = 0; index < chunks.size(); index++) {
                    String id = UUID.nameUUIDFromBytes(("financial_policy:" + source.getKey() + ":" + index)
                            .getBytes(StandardCharsets.UTF_8)).toString();
                    allChunks.add(new Document(id, chunks.get(index).getText(), Map.of(
                            "source", source.getKey(), "type", "financial_policy", "chunk", index)));
                }
            }
            return allChunks;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read knowledge resources", e);
        }
    }
}
