package com.tyson.digitalwallet.ddd.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.*;

/** Application extensions under spring.kafka; standard client properties remain owned by Boot. */
@ConfigurationProperties(prefix = "spring.kafka", ignoreUnknownFields = true)
public record KafkaBusinessProperties(
        @DefaultValue Provisioning provisioning,
        Map<String, TopicDefinition> topics,
        Map<String, String> groups) {
    public KafkaBusinessProperties {
        provisioning = provisioning == null ? new Provisioning(false) : provisioning;
        topics = topics == null ? Map.of() : Map.copyOf(topics);
        groups = groups == null ? Map.of() : Map.copyOf(groups);
        Set<String> names = new HashSet<>();
        for (var topic : topics.values())
            if (!names.add(topic.name()))
                throw new IllegalArgumentException("Duplicate Kafka topic name: " + topic.name());
        for (var group : groups.entrySet())
            if (group.getKey().isBlank() || group.getValue().isBlank())
                throw new IllegalArgumentException("Kafka group alias and ID must not be blank");
    }

    public String topicName(String alias) {
        var topic = topics.get(alias);
        if (topic == null)
            throw new IllegalArgumentException("Unknown Kafka topic alias: " + alias);
        return topic.name();
    }

    public String groupId(String alias) {
        var group = groups.get(alias);
        if (group == null)
            throw new IllegalArgumentException("Unknown Kafka group alias: " + alias);
        return group;
    }

    public record Provisioning(@DefaultValue("false") boolean enabled) {}

    public record TopicDefinition(
            String name,
            @DefaultValue("3") int partitions,
            @DefaultValue("1") short replicationFactor) {
        public TopicDefinition {
            if (name == null
                    || !name.matches("[a-zA-Z0-9._-]{1,249}")
                    || name.equals(".")
                    || name.equals(".."))
                throw new IllegalArgumentException("Invalid Kafka topic name: " + name);
            if (partitions < 1 || replicationFactor < 1)
                throw new IllegalArgumentException(
                        "Kafka partitions and replicas must be positive");
        }
    }
}
