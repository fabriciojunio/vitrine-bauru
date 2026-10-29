# Vulnerability disclosure policy

*[Leia em português](divulgacao.md)*

In force since 29/10/2026. Also published at
[`/.well-known/security.txt`](https://vitrine-bauru.vercel.app/.well-known/security.txt), in the
RFC 9116 format.

## Scope

| In | Out |
|---|---|
| `vitrine-bauru.vercel.app` | the SEDECON installation, maintained separately by the city hall: flaws there go to `desenvolvimento@bauru.sp.gov.br` |
| `vitrine-bauru-api.onrender.com` | Vercel, Render, the database provider, Resend, BrasilAPI and ViaCEP |
| the code in this repository | social engineering, physical attacks, volume-based denial of service |

## How to report

Through GitHub's private reporting:
[Report a vulnerability](https://github.com/fabriciojunio/vitrine-bauru/security/advisories/new).
Say what you found, how to reproduce it and the impact you expect. In Portuguese or English.

## What we ask

- Use the [demo](https://vitrine-bauru.vercel.app) and its accounts. Do not use, keep or disclose any
  real entrepreneur's data you happen to see.
- Stop as soon as you prove the flaw: one record read is enough; there is no need to download the
  database.
- No testing that takes the service down or deletes data.
- Do not disclose before we fix it or before 90 days from your report, whichever comes first.

## What you get

| Step | Deadline |
|---|---|
| Acknowledgement | within 7 days |
| Initial assessment and severity (CVSS 4.0) | within 14 days |
| Fix for critical or high flaws | within 30 days |
| Fix for medium or low flaws | within 90 days |
| Security advisory published on GitHub, with credit if you want it | together with the fix |

There is no cash reward: it is a project without revenue. There is public credit and thanks.

## Safe harbour

Research done in good faith, within scope and following this policy, is treated as authorised. In
that case:

- we will not take legal action against you or ask others to;
- if a third party sues you over that research, we will state publicly that it followed this policy;
- site terms of use that would prevent the research do not apply to it.

What this policy cannot do: authorise testing of third-party systems (the "out" list) or set the law
aside. If unsure whether something is in scope, ask first through the same channel.
