package com.tyson.digitalwallet.ddd.infrastructure.messaging.withdrawal;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.application.port.payment.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.impl.ReconcileWithdrawalUseCaseImpl;
import com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus;
import com.tyson.digitalwallet.ddd.infrastructure.config.*;
import com.tyson.digitalwallet.ddd.infrastructure.messaging.outbox.*;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.repository.*;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.*;
import org.springframework.boot.*;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.*;

import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;

/**
 * Real Kafka listener, offsets, DLT and PostgreSQL; provider and Redis lock port are deterministic
 * test doubles.
 */
public class WithdrawalKafkaConsumerIntegrationTest {
    private WithdrawalConsumerFixture f;
    private ConfigurableApplicationContext context;
    private String bootstrap, source, dlt, group;
    private int partitions = 1;
    private DefaultKafkaProducerFactory<Object, Object> producers;
    private KafkaTemplate<Object, Object> template;
    private Admin admin;
    private final AtomicBoolean failDlt = new AtomicBoolean();
    private final AtomicInteger failedDltSends = new AtomicInteger();
    private final ObjectMapper json = new ObjectMapper();
    private ConsumeWithdrawalEventUseCase consumer;

    @Before
    public void setup() throws Exception {
        bootstrap = System.getProperty("kafka.test.bootstrap-servers");
        var url = System.getProperty("outbox.test.jdbc");
        Assume.assumeTrue(
                "Local Kafka and isolated PostgreSQL required", bootstrap != null && url != null);
        f = new WithdrawalConsumerFixture(url);
        consumer = spy(f.consumer);
        source = "withdrawal.consumer.test." + UUID.randomUUID();
        dlt = source + ".dlt";
        group = "withdrawal-consumer-test-" + UUID.randomUUID();
        var config = new HashMap<String, Object>();
        config.put("bootstrap.servers", bootstrap);
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("acks", "all");
        config.put("enable.idempotence", true);
        config.put("max.block.ms", 5000);
        config.put("delivery.timeout.ms", 10000);
        config.put("request.timeout.ms", 5000);
        producers = new DefaultKafkaProducerFactory<>(config);
        var real = new KafkaTemplate<Object, Object>(producers);
        template = spy(real);
        doAnswer(
                        i -> {
                            ProducerRecord<Object, Object> record = i.getArgument(0);
                            if (dlt.equals(record.topic()) && failDlt.get()) {
                                failedDltSends.incrementAndGet();
                                return CompletableFuture.failedFuture(
                                        new IllegalStateException("simulated DLT send failure"));
                            }
                            return real.send(record);
                        })
                .when(template)
                .send(any(ProducerRecord.class));
        admin =
                Admin.create(
                        Map.of(
                                "bootstrap.servers",
                                bootstrap,
                                "default.api.timeout.ms",
                                10000,
                                "request.timeout.ms",
                                5000));
        startContext("kafka");
    }

    private void startContext(String mode) {
        var app =
                new SpringApplication(
                        KafkaAutoConfiguration.class,
                        KafkaTopicsConfiguration.class,
                        WithdrawalKafkaConsumerConfiguration.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setRegisterShutdownHook(false);
        app.addInitializers(
                c -> {
                    c.getBeanFactory().registerSingleton("consumerUseCase", consumer);
                    c.getBeanFactory().registerSingleton("objectMapper", json);
                    c.getBeanFactory().registerSingleton("testKafkaTemplate", template);
                });
        context =
                app.run(
                        "--spring.config.location="
                                + Path.of(
                                                "../digitalwallet-start/src/main/resources/application.yaml")
                                        .toAbsolutePath()
                                        .toUri(),
                        "--spring.kafka.bootstrap-servers=" + bootstrap,
                        "--withdrawal.processing-mode=" + mode,
                        "--spring.kafka.topics.withdrawal-process.name=" + source,
                        "--spring.kafka.topics.withdrawal-process.partitions=" + partitions,
                        "--spring.kafka.topics.withdrawal-process-dlt.name=" + dlt,
                        "--spring.kafka.topics.withdrawal-process-dlt.partitions=" + partitions,
                        "--spring.kafka.groups.withdrawal-worker=" + group,
                        "--spring.kafka.listener.concurrency=" + partitions,
                        "--spring.kafka.withdrawal-consumer.retry-interval="
                                + (partitions == 1 ? "50ms" : "500ms"),
                        "--spring.kafka.withdrawal-consumer.dlt-ack-timeout=1s",
                        "--spring.main.banner-mode=off",
                        "--logging.level.org.apache.kafka=WARN");
    }

    @After
    public void cleanup() throws Exception {
        try {
            if (context != null) context.close();
            if (producers != null) producers.destroy();
            if (admin != null) {
                try {
                    admin.deleteTopics(List.of(source, dlt)).all().get(10, TimeUnit.SECONDS);
                } finally {
                    admin.close(Duration.ofSeconds(2));
                }
            }
        } finally {
            if (f != null) f.close();
        }
    }

    private String body(UUID event, UUID id) throws Exception {
        return json.writeValueAsString(
                Map.of(
                        "eventId",
                        event.toString(),
                        "aggregateType",
                        "WITHDRAWAL",
                        "aggregateId",
                        id.toString(),
                        "eventType",
                        "WITHDRAWAL_PROCESS_REQUESTED",
                        "schemaVersion",
                        1,
                        "createdAt",
                        Instant.now().toString(),
                        "payload",
                        Map.of("withdrawalId", id.toString())));
    }

    private void send(UUID event, UUID id) throws Exception {
        template.send(source, id.toString(), body(event, id)).get(10, TimeUnit.SECONDS);
    }

    private long committed() {
        try {
            var value =
                    admin.listConsumerGroupOffsets(group)
                            .partitionsToOffsetAndMetadata()
                            .get(5, TimeUnit.SECONDS)
                            .get(new TopicPartition(source, 0));
            return value == null ? 0 : value.offset();
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > until) fail("Timed out waiting for consumer state");
            Thread.sleep(40);
        }
    }

    @Test
    public void outboxToConsumerHandlesDuplicatesRestartAndTerminalBacklog() throws Exception {
        UUID id = f.create();
        UUID event = f.jdbc.queryForObject("SELECT id FROM outbox_event", UUID.class);
        var settings =
                new OutboxPublisherProperties(
                        true,
                        20,
                        Duration.ofSeconds(12),
                        Duration.ofSeconds(1),
                        Duration.ofMinutes(5));
        var publisher =
                new OutboxPublisher(
                        f.repositories.getRepository(SpringDataOutboxEventRepository.class),
                        new KafkaOutboxMessageSender(
                                template,
                                json,
                                context.getBean(KafkaBusinessProperties.class),
                                settings),
                        settings,
                        f.manager,
                        Clock.systemUTC());
        assertEquals(1, publisher.publishBatch());
        await(() -> f.receipts() == 1 && committed() >= 1);
        assertEquals(WithdrawalStatus.PROCESSING, f.status(id));
        assertEquals(1, f.payoutCreations.get());
        context.close();
        context = null;
        consumer = spy(f.consumer);
        startContext("kafka");
        send(event, id);
        await(() -> committed() >= 2);
        assertEquals(1, f.gatewayCalls.get());
        assertEquals(1, f.receipts());
        f.outcome.set(PayoutGateway.Outcome.PAID);
        new ReconcileWithdrawalUseCaseImpl(
                        f.withdrawals,
                        mock(PayoutNotificationInbox.class),
                        f.processor,
                        new WithdrawalProcessingPolicy("kafka"))
                .reconcile();
        assertEquals(WithdrawalStatus.SUCCEEDED, f.status(id));
        assertEquals(1, f.payoutCreations.get());
        assertEquals(2, f.gatewayCalls.get());
        send(UUID.randomUUID(), id);
        await(() -> committed() >= 3 && f.receipts() == 2);
        assertEquals(2, f.gatewayCalls.get());
        verify(f.ledger, times(1)).recordWithdraw(any(), eq(f.walletId), any(), any(), anyString());
        // Scheduler mode does not register a listener at all.
        context.close();
        context = null;
        startContext("scheduler");
        assertTrue(context.getBeansOfType(WithdrawalProcessKafkaListener.class).isEmpty());
    }

    @Test
    public void concurrentConsumersDeliveringSameEventCreateOnePayout() throws Exception {
        context.close();
        context = null;
        admin.createPartitions(
                        Map.of(
                                source,
                                NewPartitions.increaseTo(2),
                                dlt,
                                NewPartitions.increaseTo(2)))
                .all()
                .get(10, TimeUnit.SECONDS);
        partitions = 2;
        startContext("kafka");
        var id = f.create();
        var event = UUID.randomUUID();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        f.beforeGateway.set(
                () -> {
                    entered.countDown();
                    try {
                        if (!release.await(10, TimeUnit.SECONDS))
                            throw new AssertionError("gateway release timeout");
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                });
        try {
            template.send(source, 0, id.toString(), body(event, id)).get(10, TimeUnit.SECONDS);
            assertTrue(entered.await(10, TimeUnit.SECONDS));
            template.send(source, 1, id.toString(), body(event, id)).get(10, TimeUnit.SECONDS);
            verify(consumer, timeout(3000).atLeast(2)).consume(event, id);
            release.countDown();
            await(
                    () -> {
                        try {
                            var offsets =
                                    admin.listConsumerGroupOffsets(group)
                                            .partitionsToOffsetAndMetadata()
                                            .get(5, TimeUnit.SECONDS);
                            return offsets.size() == 2
                                    && offsets.values().stream().allMatch(o -> o.offset() >= 1);
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    });
            assertEquals(1, f.gatewayCalls.get());
            assertEquals(1, f.payoutCreations.get());
            assertEquals(1, f.receipts());
            var ends =
                    admin.listOffsets(
                                    Map.of(
                                            new TopicPartition(dlt, 0),
                                            OffsetSpec.latest(),
                                            new TopicPartition(dlt, 1),
                                            OffsetSpec.latest()))
                            .all()
                            .get(5, TimeUnit.SECONDS);
            assertTrue(ends.values().stream().allMatch(o -> o.offset() == 0));
        } finally {
            release.countDown();
        }
    }

    @Test
    public void dltFailureDoesNotAdvanceOffsetAndProviderExhaustionRetainsFunds() throws Exception {
        failDlt.set(true);
        template.send(source, "bad-key", "broken JSON").get(10, TimeUnit.SECONDS);
        await(() -> failedDltSends.get() > 0);
        assertEquals(0, committed());
        assertEquals(0, f.receipts());
        failDlt.set(false);
        await(() -> committed() >= 1);
        var id = f.create();
        var event = UUID.randomUUID();
        f.failuresLeft.set(99);
        send(event, id);
        await(() -> committed() >= 2);
        assertEquals(3, f.gatewayCalls.get());
        assertEquals(0, f.receipts());
        assertEquals(WithdrawalStatus.PROCESSING, f.status(id));
        assertEquals(0, f.payoutCreations.get());
        assertEquals(
                0,
                new BigDecimal("800000")
                        .compareTo(
                                f.jdbc.queryForObject(
                                        "SELECT balance FROM wallets WHERE id=?",
                                        BigDecimal.class,
                                        f.walletId)));
        var cp = new HashMap<String, Object>();
        cp.put("bootstrap.servers", bootstrap);
        cp.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        cp.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        cp.put("enable.auto.commit", false);
        try (var dead = new KafkaConsumer<String, String>(cp)) {
            var partition = new TopicPartition(dlt, 0);
            dead.assign(List.of(partition));
            dead.seekToBeginning(List.of(partition));
            var records = new ArrayList<ConsumerRecord<String, String>>();
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            while (records.size() < 2 && System.nanoTime() < until)
                dead.poll(Duration.ofMillis(300)).forEach(records::add);
            assertTrue(records.stream().anyMatch(r -> "broken JSON".equals(r.value())));
            assertTrue(
                    records.stream()
                            .anyMatch(
                                    r -> {
                                        try {
                                            return event.toString()
                                                    .equals(
                                                            json.readTree(r.value())
                                                                    .path("eventId")
                                                                    .asText());
                                        } catch (Exception ex) {
                                            return false;
                                        }
                                    }));
        }
        f.failuresLeft.set(0);
        f.outcome.set(PayoutGateway.Outcome.PAID);
        new ReconcileWithdrawalUseCaseImpl(
                        f.withdrawals,
                        mock(PayoutNotificationInbox.class),
                        f.processor,
                        new WithdrawalProcessingPolicy("kafka"))
                .reconcile();
        assertEquals(WithdrawalStatus.SUCCEEDED, f.status(id));
        assertEquals(1, f.payoutCreations.get());
    }
}
