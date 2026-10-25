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

## Documentation Index

Detailed security architecture, revocation handling, and Redis ACL models are maintained in the [`docs/`](docs/) directory:

| Document | Description |
| :--- | :--- |
| 📖 [**Security Architecture & Integration**](docs/ARCHITECTURE.md) | In-depth breakdown of the OAuth2 Resource Server integration, remote Nimbus JWKS decoding with Caffeine caching, real-time token blacklisting, elevated sudo session verification, dynamic permission & role revocation listeners over Redis Pub/Sub, and least-privilege Redis ACL permissions. |

---

## Architectural Data Flow

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

## Directory Structure

```
demo/
├── pom.xml
├── README.md
├── .env.example
├── docs/
│   └── ARCHITECTURE.md
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
