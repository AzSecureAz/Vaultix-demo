package com.example.demo.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.vaultix.common.security.CustomAccessDeniedHandler;
import com.vaultix.common.security.CustomAuthenticationEntryPoint;
import com.vaultix.common.security.CustomJwtAuthenticationConverter;
import com.vaultix.common.security.JwtBlacklistValidator;
import com.vaultix.common.security.RedisTokenBlacklistService;
import com.vaultix.common.security.TokenBlacklistService;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityBeansConfig {

    @Bean
    public Cache jwkSetCache() {
        return new CaffeineCache(
                "jwk-set-cache",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .build()
        );
    }

    @Bean
    public TokenBlacklistService tokenBlacklistService(StringRedisTemplate stringRedisTemplate) {
        return new RedisTokenBlacklistService(stringRedisTemplate);
    }

    @Bean
    public JwtBlacklistValidator jwtBlacklistValidator(TokenBlacklistService tokenBlacklistService) {
        return new JwtBlacklistValidator(tokenBlacklistService);
    }

    @Bean
    public CustomJwtAuthenticationConverter customJwtAuthenticationConverter() {
        return new CustomJwtAuthenticationConverter();
    }

    @Bean
    public CustomAuthenticationEntryPoint customAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new CustomAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public CustomAccessDeniedHandler customAccessDeniedHandler(ObjectMapper objectMapper) {
        return new CustomAccessDeniedHandler(objectMapper);
    }
}
