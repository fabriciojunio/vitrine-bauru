# Threat model

*[Leia em português](modelo-de-ameacas.md)*

Reviewed on 28/10/2026, on the `homologação` branch. STRIDE applied to what runs today: the single
server on Render, the front end on Vercel and the database on Neon. The four-service version with a
gateway and Kubernetes has the same flows; what changes there is at the end.

## The system in one picture

```
 consumer ─────HTTPS──> front end (Vercel) ──HTTPS──> Spring API (Render) ──TLS──> PostgreSQL (Neon)
 business owner ─┘                                       │
 SEDECON ────────┘                                       └──HTTPS──> Resend (email), BrasilAPI (CNPJ)
```

Trust boundaries: the browser and everything that is ours; the API and the database; the API and
third-party services. GitHub Actions crosses a boundary too: it holds the demo database password
and the deploy hook, so the pipeline is part of the attack surface.

Assets, most sensitive first: the business owner's CPF or CNPJ, phone and email; password hashes
and refresh tokens; the moderation audit trail; products and photos.

## Threats

### Spoofing (S)

| Threat | Control | Evidence |
|---|---|---|
| Guessing a password | bcrypt 12, 15-minute lockout after 5 failures, 5 attempts per minute per address | `FluxoDoCadastroTest`, `SegurancaHttpTest` |
| Finding out who has an account | same answer and same timing for unknown email and wrong password | `FluxoDoCadastroTest` |
| Forging a token | HMAC with a 32+ byte secret checked at startup, issuer verified | `EmissorDeTokenTest` |
| Reusing a stolen refresh token | stored as SHA-256, rotated on every use; reuse ends every session | `FluxoDoCadastroTest` |
| Switching rate-limit bucket by forging `X-Forwarded-For` | only trusted proxies are read, right to left | `FiltroDeLimiteTest` |

### Tampering (T)

| Threat | Control | Evidence |
|---|---|---|
| SQL injection | JPA and parameterised queries, injection payload tested in search | `SegurancaHttpTest` |
| HTML with a script in a description | jsoup on input, React escaping on output | `SanitizadorTest` |
| A "photo" that is disguised HTML | type read from the first bytes, `nosniff`, random name | `TipoDeImagemTest` |
| Editing another store's product | the owner comes from the token, never the URL, and the service checks ownership | `FluxoDoCatalogoTest` |
| Fake line in the log | line breaks stripped from path and source before logging | `FiltroDeLimiteTest` |

### Repudiation (R)

Approval, rejection, suspension, reactivation, erasure requests and demo sign-ins go to the audit
table with author, date and correlation id. The record survives the data subject's erasure, because
the moderator answers for the act.

What is missing: application logs stay on Render, and anyone with access to the account can delete
them. There is no copy in a separate destination.

### Information disclosure (I)

| Threat | Control | Evidence |
|---|---|---|
| Documents and contacts in logs | documents masked; email logs a masked recipient and subject, never the body | `DocumentoTest` for the mask; email logging by code review only |
| Error details to the client | generic Problem Details with correlation id; stack trace only in the log | `SegurancaHttpTest` |
| Tracking consumers | no account, no cookie, contacts recorded without IP or session | `RegistrarContato.java` (code review) |
| XSS stealing the refresh token | CSP on the API (`default-src 'none'`) and on Vercel, limited to the exact API host since 28/10/2026 | `web/vercel.json` |

The refresh token lives in `localStorage`. It is a trade-off stated in SECURITY.md: an `HttpOnly`
cookie with front end and API on different domains would bring CSRF back. The user's email is
inside the access token, readable by whoever holds the token.

### Denial of service (D)

Per-address limits on login, sign-up, refresh, upload, search, contact and, since 28/10/2026, on
the demo entry, which writes a session per call. Uploads of 5 MB per file and 50 MB per store. The
limit lives in each instance's memory; with several replicas it multiplies.

The public demo has a risk of its own. Until 28/10/2026, any visitor could sign into the shared
store with one click and request its erasure, or suspend it by signing in as SEDECON, and the entry
button stayed broken for everyone until the nightly reset. That store now refuses both actions with
demo mode on (`DemonstracaoTest`). The nightly reset also used to drop the database before knowing
whether it could bring the application back up; the order was reversed.

### Elevation of privilege (E)

Moderation, notifications and actuator require `ADMIN_SEDECON` in the Spring Security
configuration, and `SegurancaHttpTest` checks each route with and without the role. A business owner
only reaches their own store. The role comes from the token, so demoting someone only takes effect
when the access token expires, within 15 minutes. Suspension already blocks publishing immediately,
because the catalog checks the store's status on every write.

## Pipeline

| Threat | Control |
|---|---|
| A compromised action changing the code behind a tag | every action pinned by SHA |
| The workflow token used to write to the repository | minimal `permissions` per workflow; the Render ones get none |
| A dependency with a known flaw | OSV-Scanner over the Maven SBOM and the npm lockfile, Dependabot with a 7-day cooldown |
| A secret in the history | gitleaks over the whole history on every push |

## In the four-service version

The gateway is the only exposed point, services talk over an internal network with a
`NetworkPolicy`, and pods run without root on a read-only file system. There is no mTLS between
services: inside the cluster, trust comes from the network, not from certificates.

## How this document is kept

When an authentication flow, an owned route, stored data or the pipeline changes, this file and the
[ASVS checklist](asvs.en.md) change in the same pull request.
