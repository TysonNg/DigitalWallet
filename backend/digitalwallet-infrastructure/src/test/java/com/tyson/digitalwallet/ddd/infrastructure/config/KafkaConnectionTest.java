package com.tyson.digitalwallet.ddd.infrastructure.config;

import static org.junit.Assert.*;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Starts only Kafka infrastructure: no application database, migrations, listeners or provider
 * calls.
 */
public class KafkaConnectionTest {
    @Test
    public void bootLoadsRealSettingsAndProvisionsTopicOnTheLocalBroker() throws Exception {
        String bootstrap = System.getProperty("kafka.test.bootstrap-servers");
        Assume.assumeTrue(
                "Supply an isolated local Kafka bootstrap server to run this test",
                bootstrap != null);
        String temporaryTopic = "configuration.binding." + java.util.UUID.randomUUID();
        var application =
                new SpringApplication(KafkaAutoConfiguration.class, KafkaTopicsConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setRegisterShutdownHook(false);
        String yaml =
                Path.of("../digitalwallet-start/src/main/resources/application.yaml")
                        .toAbsolutePath()
                        .toUri()
                        .toString();
        try (var context =
                application.run(
                        "--spring.config.location=" + yaml,
                        "--spring.kafka.bootstrap-servers=" + bootstrap,
                        "--spring.main.banner-mode=off",
                        "--logging.level.org.apache.kafka=WARN",
                        "--spring.kafka.topics.binding-check.name=" + temporaryTopic,
                        "--spring.kafka.topics.binding-check.partitions=2",
                        "--spring.kafka.topics.binding-check.replication-factor=1")) {
            var business = context.getBean(KafkaBusinessProperties.class);
            var consumer =
                    new ConsumerConfig(
                            context.getBean(ConsumerFactory.class).getConfigurationProperties());
            assertNull(consumer.getString(ConsumerConfig.GROUP_ID_CONFIG));
            assertEquals(
                    business.groupId("withdrawal-worker"),
                    context.getEnvironment().getProperty("spring.kafka.groups.withdrawal-worker"));
            assertEquals(
                    Boolean.FALSE, consumer.getBoolean(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
            var producer =
                    new ProducerConfig(
                            context.getBean(ProducerFactory.class).getConfigurationProperties());
            // Kafka normalizes acks=all to -1.
            assertEquals("-1", producer.getString(ProducerConfig.ACKS_CONFIG));
            assertEquals(
                    Boolean.TRUE, producer.getBoolean(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));
            var factory =
                    context.getBean(
                            "kafkaListenerContainerFactory",
                            ConcurrentKafkaListenerContainerFactory.class);
            assertEquals(
                    ContainerProperties.AckMode.RECORD,
                    factory.getContainerProperties().getAckMode());
            try (var admin =
                    Admin.create(context.getBean(KafkaAdmin.class).getConfigurationProperties())) {
                assertFalse(admin.describeCluster().nodes().get(15, TimeUnit.SECONDS).isEmpty());
                try {
                    assertEquals(3, business.topics().size());
                    var names =
                            business.topics().values().stream()
                                    .map(KafkaBusinessProperties.TopicDefinition::name)
                                    .toList();
                    var descriptions =
                            admin.describeTopics(names).allTopicNames().get(15, TimeUnit.SECONDS);
                    for (var topic : business.topics().values()) {
                        var description = descriptions.get(topic.name());
                        assertEquals(topic.partitions(), description.partitions().size());
                        for (var partition : description.partitions())
                            assertEquals(topic.replicationFactor(), partition.replicas().size());
                    }
                } finally {
                    admin.deleteTopics(List.of(temporaryTopic)).all().get(15, TimeUnit.SECONDS);
                }
            }
        }
    }
}
