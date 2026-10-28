# Security

*[Leia em português](SECURITY.md)*

The platform stores the CPF, CNPJ, phone and email of real small business owners, and it carries
the name of SEDECON, Bauru's economic development department. This document says what was done,
what was not, and how to report a problem.

## Reporting a vulnerability

Use GitHub's private reporting: the **Security** tab, **Report a vulnerability**. Describe what you
found and how to reproduce it. The first reply comes within seven days.

Do not open a public issue with details of an exploitable flaw.

This repository is the portfolio version. The SEDECON installation is maintained separately by the
city; flaws in it go to `desenvolvimento@bauru.sp.gov.br`.

## What is done

Each threat is detailed in the [threat model](docs/seguranca/modelo-de-ameacas.en.md), and the
chapter by chapter reading of OWASP ASVS 5.0 is in the [checklist](docs/seguranca/asvs.en.md).

**Passwords.** Bcrypt with cost 12. The cost is stored in the hash, so raising it later does not
invalidate existing passwords. The policy asks for eight characters and blocks the most obvious
passwords, without demanding symbols and capitals: the people using this platform are not very
familiar with digital tools, and composition rules produce "Senha@123" written on a note stuck to
the counter.

**Sessions.** Fifteen-minute access token, seven-day refresh token stored as a SHA-256 digest and
rotated on every use. A spent refresh token that shows up again ends every session of that user,
because there are only two explanations for it and both are reasons for suspicion.

**Account lockout.** Five wrong passwords lock the account for fifteen minutes. The counter
survives the exception that rejects the login, which sounds obvious and is not: the first version
lost the count in the rollback of its own transaction, and an integration test caught it.

**User enumeration.** The answer for an unknown email is identical to the one for a wrong password,
timing included: when the email does not exist, the system checks the password against a dummy
hash so the response time does not reveal what the message hides.

**Rate limiting.** Per source address, with rules per path. The filter uses `getRemoteAddr` after
Tomcat resolves trusted proxies with `server.forward-headers-strategy=native`. The chain is read
right to left: prepending a made-up IP to `X-Forwarded-For` does not create a new bucket. A
deployment must restrict `server.tomcat.remoteip.internal-proxies` and
`server.tomcat.remoteip.trusted-proxies` to its own proxies; never configure an expression that
trusts any source. On direct access by public IP, the client-supplied header is ignored.

**Uploads.** The type is detected from the first bytes of the file, never from the extension or the
declared header. A file named `photo.jpg` announced as `image/jpeg` can be HTML with a script
inside; served back on the same domain, it would run. Limits of 5 MB per file and 50 MB per store,
with a random name, which also rules out path traversal.

**Free text.** Cleaned with jsoup on input, on top of the escaping React already does on output.
Two layers because the product description also goes into email bodies and the API's JSON, which
never pass through a browser.

**SQL.** Everything through JPA or parameterised queries. No string concatenation in any query, and
there is a test with an injection payload in the search field.

**Headers.** CSP, HSTS, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff` and
`Referrer-Policy: no-referrer` on every response, checked by tests.

**CORS.** Explicit origin list, never `*`, with the domain coming from the environment. Unknown
origins are rejected, and there is a test for it.

**Secrets.** No credentials in the repository. The token secret comes from the environment and the
application refuses to start if it is shorter than 32 bytes or if the access token lifetime exceeds
one hour.

**Sensitive data in logs.** Documents appear masked on screen and in logs. The email sender logs a
masked recipient and the subject, never the body, which carries names and rejection reasons.

**Audit trail.** Who approved, rejected or suspended is recorded with date and correlation id, and
the record survives the data subject's erasure: whoever performed the act answers for it.

**Public demo.** With `DEMO_ATIVO`, the store the "business owner" button signs into refuses
erasure and suspension, so one visitor cannot take the entry point down for everyone else, and the
one-click entry is limited to ten per minute per address. Outside demo mode, the entry endpoint
answers 404.

**Dependencies and pipeline.** Since 28/10/2026, every push and pull request goes through CodeQL
(`security-extended`, Java and TypeScript), gitleaks over the whole history and OSV-Scanner over the
CycloneDX SBOM built by Maven and the npm lockfile. Spring Boot 3.5 reached the end of free support
in June 2026; the libraries it brings with known flaws (Tomcat, Jackson, the Postgres driver,
log4j-api, httpcomponents, lz4, snappy, OpenTelemetry, commons-lang3, BouncyCastle) are pinned to
fixed versions in the root `pom.xml`. The two Spring MVC flaws that only Spring 7 fixes do not apply
here and are accepted with a deadline in `osv-scanner.toml`. A finding without an entry in
`.github/codeql-accepted.json` fails the workflow, and so does an expired entry. Dependabot proposes
weekly updates with a seven-day cooldown, actions are pinned by SHA and each workflow declares only
the permissions it uses.

## What is not done, and why

**CSRF protection is off.** Authentication travels in the `Authorization` header, not in a cookie.
A token the browser does not attach on its own cannot be abused by a forged cross-site request. If
the session ever moves to a cookie, the protection has to come back, and a code comment says so.

**The refresh token lives in `localStorage`.** It is exposed to XSS. The alternative would be an
`HttpOnly` cookie, which with front end and API on different domains needs `SameSite=None` and
brings CSRF back. The trade-off was accepted, and what holds it up is the back end's CSP, the
Vercel CSP and the rule of never injecting user HTML into the page. The access token is kept in
memory only.

**Rate limits are per instance.** The bucket lives in process memory. With several instances, the
effective limit multiplies. A shared counter would need Redis, which costs money and becomes a
single point of failure in a project that must fit a free tier. Against brute force an approximate
limit is enough; against a truly distributed attempt, bcrypt and the account lockout do the work.

**No SMS phone verification.** It costs money per message. SEDECON's manual approval covers much of
the risk, which is exactly the role of the pending to approved flow.

**No bot protection on sign-up.** Cloudflare Turnstile is planned for the next phase. Today there is
the limit of ten sign-ups per hour per address and human moderation before anything goes public.

## LGPD (Brazil's data protection law)

The two rights people use most are implemented and working, each with its own API endpoint and a
button on screen:

- **See what the platform keeps**: `GET /api/cadastro/privacidade/meus-dados` returns a file with
  the account, the business and the action history.
- **Ask for erasure**: `DELETE /api/cadastro/privacidade/minha-conta` starts the saga that erases
  the data in all four services, with a deadline, retries and an alert if the legal deadline is
  missed. Details in [decision 4](docs/adr/0004-saga-de-exclusao-lgpd.md) (in Portuguese).

Nothing is kept about consumers who only browse: no account, no tracking cookie, and the contact
record has no IP, session or identifier of any kind. It counts without tracking.
