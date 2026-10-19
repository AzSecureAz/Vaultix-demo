# Vaultix Demo Resource Server

A reference microservice implementation demonstrating how to secure downstream resource servers within the **Vaultix** ecosystem using **Spring Security OAuth2 Resource Server**, **Java 27 Virtual Threads** (Project Loom), and the shared **`common`** library.

---

## Service Overview

| Attribute | Value |
| :--- | :--- |
| **Port** | `8083` |
| **Context Path** | `/api/v1/demo` |
| **Service Name** | `demo` |
| **Framework** | Spring Boot 4.1.1 + Spring Cloud 2025.1.3 |
| **Runtime** | Java 27 (Amazon Corretto) with Virtual Threads enabled |
| **Discovery** | Eureka Client (`http://localhost:8761/eureka/`) |
| **Security Mode** | Stateless OAuth2 Resource Server (`JWT`) |

---

## Architecture & Ecosystem Role

```
Client (Browser / Mobile / Gateway)
            │  Bearer JWT
            ▼
┌──────────────────────────────────────────────┐
│             Demo Resource Server             │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │ SecurityFilterChain (Stateless)        │  │
│  └───────────────────┬────────────────────┘  │
│                      ▼                       │
│  ┌────────────────────────────────────────┐  │
│  │ NimbusJwtDecoder (withJwkSetUri)       │  │
│  │   • Local Caffeine JWKS Cache (5 min)  │  │
│  │   • Issuer Validation (vaultix.jwt)    │  │
│  │   • JwtBlacklistValidator (Redis JTI)  │  │
│  └───────────────────┬────────────────────┘  │
│                      ▼                       │
│  ┌────────────────────────────────────────┐  │
│  │ CustomJwtAuthenticationConverter       │  │
│  │   • Extract actor UUID & principal     │  │
│  │   • Map roles & permissions            │  │
│  │   • On-the-fly revocation check        │  │
│  └───────────────────┬────────────────────┘  │
│                      ▼                       │
│  ┌────────────────────────────────────────┐  │
│  │ Redis Pub/Sub Revocation Listeners     │  │
│  │   • Evicts JWKS on key revocation      │  │
│  │   • Updates disabled permission cache  │  │
│  │   • Updates disabled role cache        │  │
│  └────────────────────────────────────────┘  │
└──────────────────────┬───────────────────────┘
                       │
         ┌─────────────┴─────────────┐
         ▼                           ▼
┌─────────────────┐         ┌─────────────────┐
│  Authorization  │         │   Redis (ACL)   │
│ (JWKS endpoint) │         │   (demo_user)   │
└─────────────────┘         └─────────────────┘
```

---

## Security Capabilities

### 1. Remote JWKS Decoding with Local Caching (`JwtDecoderConfig`)
- Decodes incoming JWTs signed with RSA-256 by the **Authorization Service**.
- Fetches remote keys from `http://localhost:8082/api/v1/auth/.well-known/jwks.json`.
- Uses a local **Caffeine Cache** (`jwk-set-cache`) with a 5-minute TTL to minimize network latency on high-throughput endpoints.
- Validates the expected JWT issuer (`https://auth.vaultix.com`).

### 2. Real-Time Token Blacklisting (`JwtBlacklistValidator`)
- Every incoming request's JWT `jti` is verified against the Redis blacklist before authorization checks.
- If a token was revoked on user logout or session termination, the request is rejected immediately with `401 Unauthorized`.

### 3. Dynamic Permission & Role Revocations
- **Local In-Memory Cache**: Uses Caffeine caches inside `RedisPermissionRevocationService` and `RedisRoleRevocationService` for sub-millisecond permission evaluations.
- **Startup Sync**: On boot, an `ApplicationRunner` loads all active revocations from Redis into memory.
- **Pub/Sub Event Invalidation**: Subscribes to Redis channels via `RedisPubSubHelper`:
  - `auth:signing-key:revocation`: Immediately clears the local `jwkSetCache`.
  - `auth:permission:revocation`: Updates the local disabled permissions cache in real time.
  - `auth:role:revocation`: Updates the local disabled roles cache in real time.

### 4. Custom JWT Authentication Converter
- Parses the custom `roles_permissions` JWT claim.
- Converts active, non-revoked roles into `ROLE_<name>` authorities and permissions into `PERM_<name>` authorities.
- Extracts the actor UUID as the principal identifier.

### 5. Unified Error Handling
- Standardizes all unauthorized and forbidden responses using `CustomAuthenticationEntryPoint` (401) and `CustomAccessDeniedHandler` (403), masked to prevent internal leakage and returned in `ApiResponse.error(...)` envelopes.

---

## Configuration Reference

Configured via `src/main/resources/application.yaml` or environment variables:

| Environment Variable | Default Value | Description |
| :--- | :--- | :--- |
| `PORT` | `8083` | Server HTTP port |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | `http://localhost:8761/eureka/` | Eureka server registry URL |
| `VAULTIX_JWT_ISSUER` | `https://auth.vaultix.com` | Expected JWT issuer claim (`iss`) |
| `VAULTIX_JWT_JWK_SET_URI` | `http://localhost:8082/api/v1/auth/.well-known/jwks.json` | Remote JWKS discovery endpoint |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis port |
| `SPRING_DATA_REDIS_USERNAME` | `demo_user` | Isolated Redis ACL user |
| `SPRING_DATA_REDIS_PASSWORD` | `demo_password` | Redis password |

---

## Redis ACL Permissions

The demo service connects to Redis as **`demo_user`** with restricted least-privilege permissions:
- Read/Write on own namespace: `~demo:*`
- Read-only on auth revocation stores: `%R~auth:blacklist:*`, `%R~auth:permission:disabled`, `%R~auth:role:disabled`
- Pub/Sub subscription access: `&auth:signing-key:revocation`, `&auth:permission:revocation`, `&auth:role:revocation`, `&demo:*`

Provisioning script: [`infra/redis/init-demo-acl.txt`](file:///c:/Users/vimal/Documents/Projects/Vaultix/infra/redis/init-demo-acl.txt).

---

## Directory Structure

```
demo/
├── pom.xml
├── README.md
├── .env.example
└── src/main/
    ├── java/com/example/demo/
    │   ├── DemoApplication.java
    │   └── config/
    │       ├── JwtDecoderConfig.java
    │       ├── RedisPubSubConfig.java
    │       ├── SecurityBeansConfig.java
    │       ├── SecurityConfig.java
    │       └── properties/
    │           └── JwtProperties.java
    └── resources/
        └── application.yaml
```

---

## Local Build & Execution

### 1. Build
```powershell
.\mvnw.cmd clean package
```

### 2. Run
```powershell
.\mvnw.cmd spring-boot:run
```
