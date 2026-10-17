package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vaultix.common.dto.ApiResponse;

@RestController
public class DemoController {

    @GetMapping({"/test", "/me"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getClaims(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Token claims retrieved successfully", jwt != null ? jwt.getClaims() : Map.of()));
    }
}
