package com.example.demo.config;

import org.springframework.cache.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.vaultix.common.constant.SecurityTopics;
import com.vaultix.common.security.PermissionRevocationService;
import com.vaultix.common.security.RoleRevocationService;
import com.vaultix.common.util.RedisPubSubHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            Cache jwkSetCache,
            PermissionRevocationService permissionRevocationService,
            RoleRevocationService roleRevocationService) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        RedisPubSubHelper.builder()
                .container(container)
                .topicName(SecurityTopics.SIGNING_KEY_REVOCATION)
                .onMessage(payload -> {
                    log.info("Received key revocation event on Redis: {}. Evicting local JWKS cache.", payload);
                    jwkSetCache.clear();
                })
                .build();

        RedisPubSubHelper.builder()
                .container(container)
                .topicName(SecurityTopics.PERMISSION_REVOCATION)
                .onMessage(permissionRevocationService::handleRevocationEvent)
                .build();

        RedisPubSubHelper.builder()
                .container(container)
                .topicName(SecurityTopics.ROLE_REVOCATION)
                .onMessage(roleRevocationService::handleRevocationEvent)
                .build();

        return container;
    }
}
