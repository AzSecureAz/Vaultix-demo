package com.example.demo.config;

import org.springframework.cache.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.example.demo.config.properties.JwtProperties;
import com.vaultix.common.security.CommonJwtDecoderFactory;
import com.vaultix.common.security.JwtBlacklistValidator;

@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            JwtProperties jwtProperties,
            Cache jwkSetCache,
            JwtBlacklistValidator jwtBlacklistValidator) {

        return CommonJwtDecoderFactory.create(
                jwtProperties.jwkSetUri(),
                jwkSetCache,
                jwtProperties.issuer(),
                jwtBlacklistValidator
        );
    }
}
