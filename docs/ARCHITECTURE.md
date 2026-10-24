# Vaultix Demo Resource Server - Security Architecture & Integration

Technical specification and architectural guide detailing how downstream resource servers integrate with the **Vaultix** security ecosystem using the shared `common` library, Nimbus JWT decoding, and Redis Pub/Sub revocation.

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
- Decodes incoming JWTs signed with RSA-2048 by the **Authorization Service**.
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

### 6. Platform-Wide Snake Case JSON (`SNAKE_CASE`)
- Enforces `spring.jackson.property-naming-strategy: SNAKE_CASE` across all endpoints, ensuring serialized request/response attributes match Vaultix platform conventions.

---

## Redis ACL Permissions

The demo service connects to Redis as **`demo_user`** with restricted least-privilege permissions:
- Read/Write on own namespace: `~demo:*`
- Read-only on auth revocation stores: `%R~auth:blacklist:*`, `%R~auth:permission:disabled`, `%R~auth:role:disabled`
- Pub/Sub subscription access: `&auth:signing-key:revocation`, `&auth:permission:revocation`, `&auth:role:revocation`, `&demo:*`

Provisioning script: [`infra/redis/init-demo-acl.txt`](file:///c:/Users/vimal/Documents/Projects/Vaultix/infra/redis/init-demo-acl.txt).
