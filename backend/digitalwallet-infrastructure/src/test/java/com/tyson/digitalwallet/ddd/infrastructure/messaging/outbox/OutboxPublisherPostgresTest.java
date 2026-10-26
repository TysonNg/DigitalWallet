package com.tyson.digitalwallet.ddd.infrastructure.messaging.outbox;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.application.port.outbox.OutboxEvent;
import com.tyson.digitalwallet.ddd.infrastructure.config.*;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.OutboxEventMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.repository.SpringDataOutboxEventRepository;

import org.junit.*;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.*;

import java.nio.file.*;
import java.sql.DriverManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class OutboxPublisherPostgresTest {
    private String url, schema;
    private LocalContainerEntityManagerFactoryBean factory;
    private JpaTransactionManager manager;
    private TransactionTemplate tx;
    private JdbcTemplate jdbc;
    private SpringDataOutboxEventRepository repository;
    private final Instant now = Instant.parse("2026-10-26T00:00:00Z");
    private final ObjectMapper json = new ObjectMapper();

    @Before
    public void setup() throws Exception {
        url = System.getProperty("outbox.test.jdbc");
        Assume.assumeTrue("Isolated PostgreSQL URL required", url != null);
        schema = "publisher_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var c = DriverManager.getConnection(url);
                var s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        var ds = new PGSimpleDataSource();
        ds.setURL(url);
        ds.setCurrentSchema(schema);
        jdbc = new JdbcTemplate(ds);
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(ds);
        factory.setPackagesToScan(
                "com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
        factory.afterPropertiesSet();
        var emf = Objects.requireNonNull(factory.getObject());
        manager = new JpaTransactionManager(emf);
        tx = new TransactionTemplate(manager);
        repository =
                new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf))
                        .getRepository(SpringDataOutboxEventRepository.class);
        jdbc.execute("DROP TABLE IF EXISTS outbox_event");
        jdbc.execute(
                Files.readString(
                        Path.of("src/main/resources/db/migration/V17__create_outbox_event.sql")));
    }

    @After
    public void cleanup() throws Exception {
        try {
            if (factory != null) factory.destroy();
        } finally {
            if (schema != null)
                try (var c = DriverManager.getConnection(url);
                        var s = c.createStatement()) {
                    s.execute("DROP SCHEMA " + schema + " CASCADE");
                }
        }
    }

    private UUID seed(String alias) {
        var event =
                new OutboxEvent(
                        UUID.randomUUID(),
                        "PAYMENT",
                        UUID.randomUUID().toString(),
                        "PAYMENT_COMPLETED",
                        1,
                        alias,
                        Map.of("amount", 100),
                        now.minusSeconds(1));
        tx.execute(
                s -> {
                    repository.saveAndFlush(new OutboxEventMapper(json).toEntity(event));
                    return null;
                });
        return event.id();
    }

    private OutboxPublisher worker(OutboxMessageSender sender, int batch) {
        return new OutboxPublisher(
                repository,
                sender,
                new OutboxPublisherProperties(
                        true,
                        batch,
                        Duration.ofSeconds(12),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(8)),
                manager,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    public void batchLimitAndEligibilityAreEnforcedAndSuccessClearsError() {
        var due = seed("payment");
        var future = seed("payment");
        var published = seed("payment");
        seed("payment");
        jdbc.update(
                "UPDATE outbox_event SET available_at=? WHERE id=?",
                now.plusSeconds(30).atOffset(ZoneOffset.UTC),
                future);
        jdbc.update(
                "UPDATE outbox_event SET published_at=? WHERE id=?",
                now.atOffset(ZoneOffset.UTC),
                published);
        jdbc.update("UPDATE outbox_event SET last_error='offline' WHERE id=?", due);
        var sent = new ArrayList<UUID>();
        assertEquals(1, worker(e -> sent.add(e.getId()), 1).publishBatch());
        assertEquals(1, sent.size());
        assertFalse(sent.contains(future));
        assertFalse(sent.contains(published));
        assertEquals(1, worker(e -> sent.add(e.getId()), 20).publishBatch());
        assertEquals(2, sent.size());
        assertNull(
                jdbc.queryForObject(
                        "SELECT last_error FROM outbox_event WHERE id=?", String.class, due));
        assertEquals(
                3,
                jdbc.queryForObject(
                                "SELECT count(*) FROM outbox_event WHERE published_at IS NOT NULL",
                                Integer.class)
                        .intValue());
    }

    @Test
    public void failedDeliveryPersistsBackoffAcrossWorkersAndIsNotPublished() {
        var id = seed("payment");
        var sender =
                (OutboxMessageSender)
                        e -> {
                            throw new IllegalStateException("secret payload must not be stored");
                        };
        assertEquals(1, worker(sender, 20).publishBatch());
        assertEquals(0, worker(sender, 20).publishBatch());
        var row = tx.execute(s -> repository.findById(id).orElseThrow());
        assertNull(row.getPublishedAt());
        assertEquals(1, row.getAttempts());
        assertEquals(now.plusSeconds(1), row.getAvailableAt());
        assertEquals("IllegalStateException", row.getLastError());
        jdbc.update(
                "UPDATE outbox_event SET attempts=30,available_at=? WHERE id=?",
                now.atOffset(ZoneOffset.UTC),
                id);
        assertEquals(1, worker(sender, 20).publishBatch());
        row = tx.execute(s -> repository.findById(id).orElseThrow());
        assertEquals(31, row.getAttempts());
        assertEquals(now.plusSeconds(8), row.getAvailableAt());
    }

    @Test
    public void secondWorkerSkipsLockedEventAndDoesNotDoubleSend() throws Exception {
        seed("payment");
        seed("payment");
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var first = new AtomicBoolean(true);
        var sent = ConcurrentHashMap.<UUID>newKeySet();
        var deliveries = new AtomicInteger();
        OutboxMessageSender sender =
                e -> {
                    if (first.compareAndSet(true, false)) {
                        entered.countDown();
                        if (!release.await(10, TimeUnit.SECONDS)) throw new TimeoutException();
                    }
                    assertTrue(sent.add(e.getId()));
                    deliveries.incrementAndGet();
                };
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> worker(sender, 2).publishBatch());
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var b = pool.submit(() -> worker(sender, 2).publishBatch());
            assertEquals(1, b.get(5, TimeUnit.SECONDS).intValue());
            assertEquals(1, deliveries.get());
            release.countDown();
            assertEquals(1, a.get(5, TimeUnit.SECONDS).intValue());
            assertEquals(2, deliveries.get());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    public void acknowledgementFollowedByDatabaseRollbackIsRetriedWithSameEventId() {
        var id = seed("payment");
        var ids = new ArrayList<UUID>();
        assertThrows(
                IllegalStateException.class,
                () ->
                        worker(
                                        e -> {
                                            ids.add(e.getId());
                                            TransactionSynchronizationManager
                                                    .registerSynchronization(
                                                            new TransactionSynchronization() {
                                                                @Override
                                                                public void beforeCommit(
                                                                        boolean readOnly) {
                                                                    throw new IllegalStateException(
                                                                            "simulated crash before"
                                                                                + " commit");
                                                                }
                                                            });
                                        },
                                        1)
                                .publishBatch());
        assertNull(
                jdbc.queryForObject(
                        "SELECT published_at FROM outbox_event WHERE id=?",
                        java.sql.Timestamp.class,
                        id));
        assertEquals(1, worker(e -> ids.add(e.getId()), 1).publishBatch());
        assertEquals(List.of(id, id), ids);
    }

    @Test
    public void interruptPersistsRetryAndStopsBatch() {
        seed("payment");
        seed("payment");
        try {
            assertEquals(
                    1,
                    worker(
                                    e -> {
                                        throw new InterruptedException();
                                    },
                                    20)
                            .publishBatch());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        assertEquals(
                1,
                jdbc.queryForObject(
                                "SELECT count(*) FROM outbox_event WHERE attempts=1", Integer.class)
                        .intValue());
    }

    @Test
    public void publishesFromPostgresToRealKafkaAndConsumerReadsEnvelope() throws Exception {
        var bootstrap = System.getProperty("kafka.test.bootstrap-servers");
        Assume.assumeTrue("Local Kafka required", bootstrap != null);
        var topic = "outbox.integration." + UUID.randomUUID();
        var config = new HashMap<String, Object>();
        config.put("bootstrap.servers", bootstrap);
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("acks", "all");
        config.put("enable.idempotence", true);
        config.put("max.block.ms", 5000);
        config.put("delivery.timeout.ms", 10000);
        config.put("request.timeout.ms", 5000);
        try (var admin =
                org.apache.kafka.clients.admin.Admin.create(
                        Map.of(
                                "bootstrap.servers",
                                bootstrap,
                                "default.api.timeout.ms",
                                10000,
                                "request.timeout.ms",
                                5000))) {
            admin.createTopics(
                            List.of(
                                    new org.apache.kafka.clients.admin.NewTopic(
                                            topic, 1, (short) 1)))
                    .all()
                    .get(15, TimeUnit.SECONDS);
            var pf =
                    new org.springframework.kafka.core.DefaultKafkaProducerFactory<Object, Object>(
                            config);
            try {
                var template = new org.springframework.kafka.core.KafkaTemplate<Object, Object>(pf);
                var topics =
                        new KafkaBusinessProperties(
                                null,
                                Map.of(
                                        "payment",
                                        new KafkaBusinessProperties.TopicDefinition(
                                                topic, 1, (short) 1)),
                                null);
                var settings =
                        new OutboxPublisherProperties(
                                true,
                                1,
                                Duration.ofSeconds(12),
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(8));
                var id = seed("payment");
                assertEquals(
                        1,
                        worker(new KafkaOutboxMessageSender(template, json, topics, settings), 1)
                                .publishBatch());
                assertNotNull(
                        jdbc.queryForObject(
                                "SELECT published_at FROM outbox_event WHERE id=?",
                                java.sql.Timestamp.class,
                                id));
                var cp = new HashMap<String, Object>();
                cp.put("bootstrap.servers", bootstrap);
                cp.put(
                        "key.deserializer",
                        "org.apache.kafka.common.serialization.StringDeserializer");
                cp.put(
                        "value.deserializer",
                        "org.apache.kafka.common.serialization.StringDeserializer");
                cp.put("enable.auto.commit", false);
                try (var consumer =
                        new org.apache.kafka.clients.consumer.KafkaConsumer<String, String>(cp)) {
                    var partition = new org.apache.kafka.common.TopicPartition(topic, 0);
                    consumer.assign(List.of(partition));
                    consumer.seekToBeginning(List.of(partition));
                    var records =
                            new ArrayList<
                                    org.apache.kafka.clients.consumer.ConsumerRecord<
                                            String, String>>();
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
                    while (records.isEmpty() && System.nanoTime() < deadline)
                        consumer.poll(Duration.ofMillis(500)).forEach(records::add);
                    assertEquals(1, records.size());
                    var record = records.getFirst();
                    var tree = json.readTree(record.value());
                    assertEquals(id.toString(), tree.path("eventId").asText());
                    assertEquals(record.key(), tree.path("aggregateId").asText());
                    assertEquals(100, tree.path("payload").path("amount").asInt());
                    assertEquals("PAYMENT_COMPLETED", tree.path("eventType").asText());
                }
            } finally {
                pf.destroy();
                admin.deleteTopics(List.of(topic)).all().get(15, TimeUnit.SECONDS);
            }
        }
    }
}
