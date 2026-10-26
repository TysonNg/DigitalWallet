package com.tyson.digitalwallet.ddd.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.application.exception.InvalidWithdrawalEventException;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.ConsumeWithdrawalEventUseCase;
import com.tyson.digitalwallet.ddd.infrastructure.messaging.withdrawal.WithdrawalProcessKafkaListener;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Properties;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "withdrawal", name = "processing-mode", havingValue = "kafka")
@EnableConfigurationProperties(WithdrawalKafkaConsumerProperties.class)
public class WithdrawalKafkaConsumerConfiguration {
    @Bean
    public WithdrawalProcessKafkaListener withdrawalProcessKafkaListener(
            ObjectMapper json, ConsumeWithdrawalEventUseCase consumer) {
        return new WithdrawalProcessKafkaListener(json, consumer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object>
            withdrawalKafkaListenerContainerFactory(
                    ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
                    ConsumerFactory<Object, Object> consumers,
                    KafkaTemplate<Object, Object> template,
                    KafkaBusinessProperties topics,
                    WithdrawalKafkaConsumerProperties settings) {
        var source = topics.topics().get("withdrawal-process");
        var dlt = topics.topics().get("withdrawal-process-dlt");
        if (source == null || dlt == null || dlt.partitions() < source.partitions())
            throw new IllegalArgumentException(
                    "Withdrawal DLT requires at least the source topic partition count");
        var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
        configurer.configure(factory, consumers);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.getContainerProperties().setSyncCommits(true);
        var overrides = new Properties();
        overrides.setProperty("enable.auto.commit", "false");
        overrides.setProperty("max.poll.records", "1");
        overrides.setProperty("max.poll.interval.ms", "600000");
        factory.getContainerProperties().setKafkaConsumerProperties(overrides);
        var recoverer =
                new DeadLetterPublishingRecoverer(
                        template,
                        (record, error) -> new TopicPartition(dlt.name(), record.partition()));
        recoverer.setFailIfSendResultIsError(true);
        recoverer.setWaitForSendResultTimeout(settings.dltAckTimeout());
        var handler =
                new DefaultErrorHandler(
                        recoverer,
                        new FixedBackOff(settings.retryInterval().toMillis(), settings.retries()));
        handler.addNotRetryableExceptions(InvalidWithdrawalEventException.class);
        factory.setCommonErrorHandler(handler);
        return factory;
    }
}
