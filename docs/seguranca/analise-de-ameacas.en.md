# Threat analysis: data flow, attack tree, PASTA and DREAD

*[Leia em português](analise-de-ameacas.md)*

Reviewed on 29/10/2026. Complements the [STRIDE model](modelo-de-ameacas.en.md): there, each threat
has the control and the test that proves it; here, the most serious scenario is seen from three
angles, with scoring to prioritise and the requirements that come out of it.

## 1. Data flow diagram

```
                 ┌────────────────────── TB1: internet ──────────────────────┐
  [consumer]     │                                                            │
  [entrepreneur] ├─HTTPS─> (1) React front ──HTTPS + Bearer──> (2) Spring API │
  [moderator]    │         Vercel, CSP                        Render          │
                 └────────────────────────────────────────────────┬──────────┘
                                                                   │
                 ┌──────────── TB2: data ──────────────┐           │
                 │  [D1] managed PostgreSQL ◄──TLS─────┼───────────┤
                 │   accounts, profiles, catalogue,    │           │
                 │   photos, audit, outbox/inbox       │           │
                 └─────────────────────────────────────┘           │
                 ┌──────────── TB3: third parties ─────┐           │
                 │  (3) Resend: email   ◄──HTTPS───────┼───────────┤
                 │  (4) BrasilAPI: CNPJ ◄──HTTPS───────┼───────────┘
                 │  (5) ViaCEP: postcode ◄──HTTPS── straight from the front (1)
                 └─────────────────────────────────────┘
                 ┌──────────── TB4: delivery ──────────┐
                 │  GitHub Actions: demo database password,
                 │  Render Deploy Hook, Vercel deploy
                 └─────────────────────────────────────┘
```

| Flow | Data | Who authenticates whom |
|---|---|---|
| Browser → front | nothing sensitive; static HTML and JS | Vercel TLS |
| Front → API | credentials at login; then access token (15 min) and refresh token | Render TLS; the API validates the token HMAC |
| API → database | everything, including documents, contacts and password hashes | TLS and database user |
| API → Resend | recipient and email body | Resend API key |
| API → BrasilAPI | CNPJ (company number) entered at sign-up | none (public API) |
| Browser → ViaCEP | postcode typed at sign-up, straight from the front | none (public API); allowed in the CSP `connect-src` |
| CI → Render, Neon | code, demo database password | GitHub secrets |

## 2. Most serious scenario and attack tree

**Attacker goal:** obtain every entrepreneur's documents and contacts, or approve fake shops for
scams with the city hall's seal. Both go through the same asset: a moderator session
(`ADMIN_SEDECON`).

```
GOAL: act as a SEDECON moderator
├── OR 1. Steal the moderator's credentials
│   ├── 1.1 Brute force on login ............... lockout after 5 errors + per-address limit [mitigated]
│   ├── 1.2 Credentials leaked from another site  no second factor [OPEN, R-01]
│   └── 1.3 Phishing of the login page ......... no phishing-resistant second factor [OPEN, R-01]
├── OR 2. Steal an issued token
│   ├── 2.1 XSS in the front reading localStorage  strict CSP + React escaping + jsoup [mitigated]
│   │   └── AND 2.1.1 compromised front dependency  OSV + Dependabot 7 days [mitigated, residual]
│   └── 2.2 Token in a log or URL .............. token only in the header; logs without headers [mitigated]
├── OR 3. Forge or elevate the token
│   ├── 3.1 Forge the HMAC ..................... 32+ byte secret validated at startup [mitigated]
│   ├── 3.2 Role taken from the request body ... role only from the token [mitigated]
│   └── 3.3 Entrepreneur calling a moderation route  hasRole per route + test [mitigated]
└── OR 4. Go around the application
    ├── 4.1 Database password in the pipeline .. GitHub secret, minimal permissions [mitigated]
    │   └── AND 4.1.1 compromised action ....... actions pinned by SHA [mitigated]
    └── 4.2 Read the database via SQL injection  JPA with parameters + payload test [mitigated]
```

Both open branches are the same requirement: a second factor for the moderator (R-01).

## 3. PASTA, one round

| Stage | Output |
|---|---|
| 1. Business objectives | Give local entrepreneurs visibility with SEDECON's endorsement; the endorsement is only worth anything if moderation can be trusted; personal data handled under the LGPD |
| 2. Technical scope | React front (Vercel), Spring Boot 3 API in one process (Render), managed PostgreSQL, Resend, BrasilAPI, GitHub Actions |
| 3. Decomposition | Section 1: four boundaries, seven flows, assets in [modelo-de-ameacas.en.md](modelo-de-ameacas.en.md) |
| 4. Threats | Fraud through an approved fake shop; mass harvesting of contacts for spam or scams; taking the public showcase down on the eve of a fair |
| 5. Vulnerabilities | No second factor (R-01); per-instance rate limit (R-04); logs only on Render (R-03); refresh token in `localStorage` (ADR 0010) |
| 6. Attack | Tree in section 2; the cheapest path is reused moderator credentials (1.2) |
| 7. Risk and impact | City hall seal on a scam shop: damage to SEDECON's reputation and to citizens; CPF and phone leak: an incident reportable to the ANPD |

## 4. DREAD

Scale of 1 to 3 per criterion (damage, reproducibility, exploitability, affected users,
discoverability). Used only for ordering; deadlines are set by the [risk register](riscos.en.md).

| Threat | D | R | E | A | D | Total | Status |
|---|---|---|---|---|---|---|---|
| Reused moderator credentials (1.2) | 3 | 3 | 2 | 3 | 2 | 13 | open, R-01 |
| Mass harvesting of the public showcase by a bot | 2 | 3 | 3 | 2 | 3 | 13 | partial: per-address limit, R-04 |
| XSS through a compromised dependency (2.1.1) | 3 | 1 | 1 | 3 | 1 | 9 | residual |
| Moderator erases traces in the application log | 2 | 2 | 1 | 1 | 1 | 7 | open, R-03 |
| Forging a token (3.1) | 3 | 1 | 1 | 3 | 1 | 9 | mitigated |
| Taking the API down by volume | 2 | 3 | 2 | 3 | 3 | 13 | partial: per-instance limit and Render, R-04 |

DREAD has a reputation for being subjective, and it is: "discoverability" penalises what is obvious,
and what is obvious should already be fixed. It is here because the career plan asks for it and
some companies still use it; for decisions, CVSS and the risk register are better.

## 5. Derived security requirements

| Id | Requirement | Threat | Status | Verification |
|---|---|---|---|---|
| RS-01 | Moderators sign in with a second factor (TOTP or passkey) | 1.2, 1.3 | pending (R-01) | test that login without the second factor is refused |
| RS-02 | Every moderation route requires `ADMIN_SEDECON` on the server | 3.3 | met | `SegurancaHttpTest` |
| RS-03 | Resource owner comes from the token, never from the URL or body | 3.2 | met | `FluxoDoCatalogoTest` |
| RS-04 | Moderation actions recorded with author, date and correlation, and the record survives the data subject's erasure | repudiation | met | `FluxoDoCadastroTest.auditoriaRegistra`, `JornadaCompletaTest` (account anonymised, audit kept) |
| RS-05 | Audit log copied to a destination the application account cannot delete | moderator erases traces | pending (R-03) | |
| RS-06 | Rate limit shared across instances | mass harvesting, DoS | pending (R-04) | |
| RS-07 | CSP without `unsafe-inline` for scripts, restricted to the API host | 2.1 | met | `web/vercel.json`, ZAP |
| RS-08 | New dependencies only after 7 days and without known flaws | 2.1.1 | met | OSV-Scanner, Dependabot |
| RS-09 | Pipeline secrets with minimal scope, actions pinned by SHA | 4.1 | met | workflow review |
| RS-10 | Database queries only parameterised | 4.2 | met | `SegurancaHttpTest` (injection payload) |
