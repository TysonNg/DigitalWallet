package com.tyson.digitalwallet.ddd.infrastructure.persistence.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory lettuceConnectionFactory() {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration("server", 6379));
    }

}
