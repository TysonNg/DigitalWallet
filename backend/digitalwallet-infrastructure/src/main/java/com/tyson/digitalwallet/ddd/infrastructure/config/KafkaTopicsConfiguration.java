package com.tyson.digitalwallet.ddd.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KafkaBusinessProperties.class)
public class KafkaTopicsConfiguration {
    @Bean
    @ConditionalOnProperty(
            prefix = "spring.kafka.provisioning",
            name = "enabled",
            havingValue = "true")
    public KafkaAdmin.NewTopics applicationKafkaTopics(KafkaBusinessProperties properties) {
        NewTopic[] topics =
                properties.topics().values().stream()
                        .map(
                                topic ->
                                        TopicBuilder.name(topic.name())
                                                .partitions(topic.partitions())
                                                .replicas(topic.replicationFactor())
                                                .build())
                        .toArray(NewTopic[]::new);
        return new KafkaAdmin.NewTopics(topics);
    }
}
