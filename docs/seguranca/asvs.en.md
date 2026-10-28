# OWASP ASVS 5.0 checklist

*[Leia em português](asvs.md)*

Assessed on 28/10/2026 chapter by chapter, targeting level 2. It is not a certification: it is a
reading of the code against the standard, with the file or test that backs each answer. "Partial"
means part of the chapter is covered and the missing part is written down.

| Chapter | Status | Evidence and what is missing |
|---|---|---|
| V1 Encoding and sanitization | Meets | Bean Validation on every body; jsoup on free text; JPA with parameters; line breaks stripped before logging (`SanitizadorTest`, `FiltroDeLimiteTest`). |
| V2 Validation and business logic | Meets | Moderation as a state machine in the domain (`EmpreendedorTest`); the demo entry store cannot be erased or suspended (`DemonstracaoTest`). |
| V3 Web frontend security | Partial | CSP on the API and on Vercel limited to the API host, HSTS, `nosniff`, `frame-ancestors 'none'`. Missing: the refresh token lives in `localStorage`, and the Vercel CSP still allows `style-src 'unsafe-inline'`. |
| V4 API and web services | Meets | Explicit CORS list, cookie-less API (CSRF does not apply, accepted in `.github/codeql-accepted.json`), per-address limits on sensitive routes. |
| V5 File handling | Meets | Type from the bytes, 5 MB per file and 50 MB per store, image in the database with a random name and `nosniff` on delivery (`TipoDeImagemTest`). |
| V6 Authentication | Partial | bcrypt 12, lockout after 5 failures, same answer and timing for unknown accounts. Missing: 2FA for SEDECON and a breached-password check. |
| V7 Session management | Meets | 15-minute access token in memory, 7-day refresh token stored as SHA-256, rotation and reuse detection; erasure ends all sessions. |
| V8 Authorization | Meets | Deny by default, role per route checked in `SegurancaHttpTest`, store owner always taken from the token. |
| V9 Self-contained tokens | Partial | 32+ byte secret checked at startup, issuer and expiry verified. Missing: `aud` in the token, and the user's email travels in the token body. |
| V10 OAuth and OIDC | Not applicable | The API issues its own tokens. |
| V11 Cryptography | Meets | No hand-rolled encryption; bcrypt for passwords, SHA-256 for refresh tokens. TLS to the database comes from `sslmode=require` in the deployment URL (`docs/IMPLANTACAO.md`); the code does not enforce it. |
| V12 Secure communication | Meets in production | HTTPS on Vercel and Render, TLS on Neon. The Kubernetes version has no mTLS between services. |
| V13 Configuration | Meets | Secrets only from the environment, the app refuses to start with a short secret, non-root image, demo mode off by default, actions pinned by SHA. |
| V14 Data protection | Meets | LGPD export and erasure with a saga and deadline (ADR 0004); masked documents; no consumer tracking. |
| V15 Secure coding and architecture | Meets | CodeQL `security-extended` on Java and TypeScript, OSV over the SBOM, gitleaks, Dependabot with a 7-day cooldown, ArchUnit keeping layers apart. Spring Boot 3.5 libraries with known flaws are pinned to fixed versions in `pom.xml`. |
| V16 Logging and error handling | Partial | JSON with correlation id, generic error to the client, moderation audit trail. Missing: logs shipped off the server and alerting. |
| V17 WebRTC | Not applicable | |

## What comes first

By risk: moving to Spring Boot 4, because the 3.5 line gets no more free fixes and the two accepted
Spring MVC flaws expire on 28/01/2027; 2FA for the SEDECON account, which moderates the whole city;
and logs off the server.
