package com.tyson.digitalwallet.ddd.infrastructure.config;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.*;

public class KafkaTopicsConfigurationTest {
    private AnnotationConfigApplicationContext context(Map<String, Object> values) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment()
                .getPropertySources()
                .addFirst(new MapPropertySource("test", values));
        context.register(KafkaTopicsConfiguration.class);
        context.refresh();
        return context;
    }

    @Test
    public void bindsMultipleBusinessTopicsAndGroupsAlongsideStandardBootSettings() {
        try (var c =
                context(
                        Map.of(
                                "spring.kafka.bootstrap-servers", "localhost:9092",
                                "spring.kafka.provisioning.enabled", "true",
                                "spring.kafka.topics.withdrawal-process.name",
                                        "wallet.withdrawal.process.v1",
                                "spring.kafka.topics.withdrawal-process.partitions", "3",
                                "spring.kafka.topics.withdrawal-process.replication-factor", "1",
                                "spring.kafka.topics.payment-completed.name",
                                        "wallet.payment.completed.v1",
                                "spring.kafka.topics.payment-completed.partitions", "6",
                                "spring.kafka.topics.payment-completed.replication-factor", "2",
                                "spring.kafka.groups.withdrawal-worker", "withdrawal-workers",
                                "spring.kafka.groups.notification-worker",
                                        "notification-workers"))) {
            var p = c.getBean(KafkaBusinessProperties.class);
            assertEquals(2, p.topics().size());
            assertEquals("wallet.withdrawal.process.v1", p.topicName("withdrawal-process"));
            assertEquals(6, p.topics().get("payment-completed").partitions());
            assertEquals("notification-workers", p.groupId("notification-worker"));
            assertNotNull(c.getBean(KafkaAdmin.NewTopics.class));
        }
    }

    @Test
    public void provisioningDisabledStillBindsBusinessConfiguration() {
        try (var c =
                context(
                        Map.of(
                                "spring.kafka.provisioning.enabled",
                                "false",
                                "spring.kafka.topics.withdrawal-process.name",
                                "wallet.withdrawal.process.v1"))) {
            assertTrue(c.getBeansOfType(KafkaAdmin.NewTopics.class).isEmpty());
            assertEquals(
                    "wallet.withdrawal.process.v1",
                    c.getBean(KafkaBusinessProperties.class).topicName("withdrawal-process"));
        }
    }

    @Test
    public void provisioningRequiresExplicitOptIn() {
        try (var c = context(Map.of())) {
            assertTrue(c.getBeansOfType(KafkaAdmin.NewTopics.class).isEmpty());
        }
    }

    @Test
    public void invalidTopicDefinitionsFailBeforeBrokerCalls() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaBusinessProperties.TopicDefinition("", 3, (short) 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaBusinessProperties.TopicDefinition("bad topic", 3, (short) 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaBusinessProperties.TopicDefinition("valid", 0, (short) 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaBusinessProperties.TopicDefinition("valid", 3, (short) 0));
    }

    @Test
    public void duplicatePhysicalTopicNamesAndBlankGroupIdsAreRejected() {
        var topic = new KafkaBusinessProperties.TopicDefinition("same.topic", 3, (short) 1);
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaBusinessProperties(null, Map.of("a", topic, "b", topic), Map.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaBusinessProperties(null, Map.of(), Map.of("worker", " ")));
    }

    @Test
    public void unknownAliasesFailClearly() {
        var p = new KafkaBusinessProperties(null, Map.of(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> p.topicName("missing"));
        assertThrows(IllegalArgumentException.class, () -> p.groupId("missing"));
    }
}
