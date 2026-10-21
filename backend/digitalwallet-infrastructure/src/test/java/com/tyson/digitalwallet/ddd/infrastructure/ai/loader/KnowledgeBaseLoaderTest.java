package com.tyson.digitalwallet.ddd.infrastructure.ai.loader;

import com.tyson.digitalwallet.ddd.infrastructure.ai.service.LocalKnowledgeBaseService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class KnowledgeBaseLoaderTest {
    private JdbcTemplate jdbc;
    private LocalKnowledgeBaseService local;
    private ResourcePatternResolver resources;
    private KnowledgeBaseLoader loader;
    private final AtomicBoolean failEmbedding = new AtomicBoolean();
    private final AtomicReference<List<Document>> added = new AtomicReference<>();

    @Before
    public void setUp() throws Exception {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE chunks (id VARCHAR PRIMARY KEY, content VARCHAR, policy_type VARCHAR)");
        local = mock(LocalKnowledgeBaseService.class);
        resources = mock(ResourcePatternResolver.class);
        VectorStore store = mock(VectorStore.class);
        // Preserve real JDBC side effects so a missing transaction genuinely loses data.
        doAnswer(invocation -> {
            assertEquals("type == 'financial_policy'", invocation.getArgument(0));
            jdbc.update("DELETE FROM chunks WHERE policy_type = 'financial_policy'");
            return null;
        }).when(store).delete(anyString());
        doAnswer(invocation -> {
            List<Document> documents = invocation.getArgument(0);
            added.set(documents);
            for (Document document : documents) {
                jdbc.update("INSERT INTO chunks VALUES (?, ?, ?)", document.getId(),
                        document.getText(), document.getMetadata().get("type"));
                if (failEmbedding.get()) {
                    throw new IllegalStateException("embedding provider failed after partial write");
                }
            }
            return null;
        }).when(store).add(anyList());
        loader = new KnowledgeBaseLoader(store, jdbc, local, resources);
    }

    @Test
    public void offlineSyncAndDirectIngestRefuseBeforeReadingOrMutating() throws Exception {
        when(local.isOfflineMode()).thenReturn(true);
        seed("old", "old policy", "financial_policy");
        assertTrue(assertThrows(IllegalStateException.class, loader::sync).getMessage().contains("offline"));
        assertThrows(IllegalStateException.class, loader::ingestKnowledgeBase);
        loader.run(null);
        assertEquals(List.of("old policy"), contents());
        verifyNoInteractions(resources);
    }

    @Test
    public void repeatedSyncReplacesRemovedAndShortenedDocumentsWithStableChunks() throws Exception {
        when(resources.getResources(anyString())).thenReturn(new Resource[] {
                markdown("fees.md", "A fee policy. ".repeat(1500)),
                markdown("removed.md", "A removed policy. ".repeat(50))});
        assertTrue(loader.sync() > 2);
        seed("unrelated", "keep this", "other");
        when(resources.getResources(anyString())).thenReturn(new Resource[] {
                markdown("fees.md", "The current short fee policy.")});
        assertEquals(1, loader.sync());
        Document chunk = added.get().getFirst();
        assertEquals("fees.md", chunk.getMetadata().get("source"));
        assertEquals("financial_policy", chunk.getMetadata().get("type"));
        assertEquals(0, chunk.getMetadata().get("chunk"));
        UUID.fromString(chunk.getId());
        var metadata = java.util.Map.copyOf(chunk.getMetadata());
        assertEquals(1, loader.sync());
        assertEquals(chunk.getId(), added.get().getFirst().getId());
        assertEquals(metadata, added.get().getFirst().getMetadata());
        assertEquals(List.of("The current short fee policy.", "keep this"), contents());
    }

    @Test
    public void embeddingFailureRollsBackDeletionAndPartialInsertForBothEntryPoints() throws Exception {
        seed("old", "old policy", "financial_policy");
        when(resources.getResources(anyString())).thenReturn(new Resource[] {
                markdown("fees.md", "A replacement policy.")});
        failEmbedding.set(true);
        assertThrows(RuntimeException.class, loader::sync);
        assertEquals(List.of("old policy"), contents());
        assertThrows(RuntimeException.class, loader::ingestKnowledgeBase);
        assertEquals(List.of("old policy"), contents());
    }

    @Test
    public void emptyResourceSetRemovesAllObsoletePolicyChunksOnly() throws Exception {
        seed("old", "old policy", "financial_policy");
        seed("other", "keep this", "other");
        when(resources.getResources(anyString())).thenReturn(new Resource[0]);
        assertEquals(0, loader.sync());
        assertEquals(List.of("keep this"), contents());
    }

    @Test
    public void duplicateClasspathResourcesProduceOnlyOneChunk() throws Exception {
        when(resources.getResources(anyString())).thenReturn(new Resource[] {
                markdown("fees.md", "The current fee policy."),
                markdown("fees.md", "The current fee policy.")});
        assertEquals(1, loader.sync());
        assertEquals(List.of("The current fee policy."), contents());
    }

    @Test
    public void conflictingResourceNamesFailWithoutDeletingExistingChunks() throws Exception {
        seed("old", "old policy", "financial_policy");
        when(resources.getResources(anyString())).thenReturn(new Resource[] {
                markdown("fees.md", "One fee policy."),
                markdown("fees.md", "A different fee policy.")});
        assertThrows(RuntimeException.class, loader::sync);
        assertEquals(List.of("old policy"), contents());
    }

    @Test
    public void blankDocumentRemovesItsObsoleteChunks() throws Exception {
        seed("old", "old policy", "financial_policy");
        when(resources.getResources(anyString())).thenReturn(new Resource[] {markdown("fees.md", " \n ")});
        assertEquals(0, loader.sync());
        assertTrue(contents().isEmpty());
    }

    @Test
    public void unreadableResourcePreservesExistingChunks() throws Exception {
        seed("old", "old policy", "financial_policy");
        Resource broken = mock(Resource.class);
        when(broken.getFilename()).thenReturn("broken.md");
        when(broken.getInputStream()).thenThrow(new java.io.IOException("unreadable"));
        when(resources.getResources(anyString())).thenReturn(new Resource[] {broken});
        assertThrows(RuntimeException.class, loader::sync);
        assertEquals(List.of("old policy"), contents());
    }

    private void seed(String id, String text, String type) {
        jdbc.update("INSERT INTO chunks VALUES (?, ?, ?)", id, text, type);
    }

    private List<String> contents() {
        return jdbc.queryForList("SELECT content FROM chunks ORDER BY content", String.class);
    }

    private Resource markdown(String name, String text) {
        return new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8)) {
            @Override public String getFilename() { return name; }
        };
    }
}
