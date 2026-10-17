package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.vaultix.common.security.CommonJwtDecoderFactory;
import com.vaultix.common.security.JwtBlacklistValidator;

@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${vaultix.jwt.issuer}") String issuer,
            Cache jwkSetCache,
            JwtBlacklistValidator jwtBlacklistValidator) {

        return CommonJwtDecoderFactory.create(jwkSetUri, jwkSetCache, issuer, jwtBlacklistValidator);
    }
}
