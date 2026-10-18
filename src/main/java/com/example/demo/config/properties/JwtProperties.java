package com.example.demo.config.properties;

import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "vaultix.jwt")
public record JwtProperties(
        @NotBlank(message = "Token issuer is required")
        @URL(message = "Token issuer must be a valid URL")
        @Size(max = 1024, message = "Token issuer cannot exceed 1024 characters")
        @DefaultValue("https://auth.vaultix.com")
        String issuer,
        @NotBlank(message = "JWK Set URI is required")
        @URL(message = "JWK Set URI must be a valid URL")
        @Size(max = 1024, message = "JWK Set URI cannot exceed 1024 characters")
        @DefaultValue("http://localhost:8082/api/v1/auth/.well-known/jwks.json")
        String jwkSetUri) {

}
