# Risk register, exceptions and release decision

*[Leia em português](riscos.md)*

Reviewed on 29/10/2026. A risk enters this register when it has been seen and will not be fixed now.
Without the register, "accepted" turns into "forgotten".

## Open risks

Likelihood and impact from 1 to 3; the level is the product (1-2 low, 3-4 medium, 6-9 high).

| Id | Risk | L | I | Level | Treatment | Owner | Deadline | Next review |
|---|---|---|---|---|---|---|---|---|
| R-01 | Moderator without a second factor: reused credentials give access to moderation and documents | 2 | 3 | high (6) | reduce: TOTP for `ADMIN_SEDECON` | Fabrício | 31/12/2026 | 30/11/2026 |
| R-02 | Refresh token in `localStorage`, readable by XSS | 1 | 3 | medium (3) | accept, [ADR 0010](../adr/0010-token-de-renovacao-no-armazenamento-local.md); relies on the CSP | Fabrício | review every 6 months | 29/04/2027 |
| R-03 | Application log only on Render: whoever holds the account can erase traces | 2 | 2 | medium (4) | reduce: copy to a separate, append-only destination | Fabrício | 31/03/2027 | 31/12/2026 |
| R-04 | In-memory rate limit: with more than one instance, it multiplies | 1 | 2 | low (2) | accept while there is one instance; reduce before scaling | Fabrício | before the second instance | 29/04/2027 |
| R-06 | User email inside the access token, readable by whoever holds the token | 1 | 1 | low (1) | accept: the token already gives access to the whole account | Fabrício | review every 6 months | 29/04/2027 |

## Tool exceptions

A tool finding that will not be fixed is also an accepted risk, just a smaller one. These live in the
tool's own files, always with a reason and a deadline:

| Where | What it holds |
|---|---|
| [`osv-scanner.toml`](../../osv-scanner.toml) | accepted dependency vulnerabilities, with `ignoreUntil` |
| `.github/codeql-accepted.json` | accepted CodeQL alerts, with reason, mitigation and review date |
| `.github/workflows/dast.yml` and the ZAP rules file | DAST alerts triaged as false positives |

An exception without a deadline is not accepted in code review.

## Release decision

A version goes from `homologação` to `produção` when every condition below is true. They are checked
by CI, not by the approver's memory.

| Condition | Where it is checked |
|---|---|
| Back-end and front-end tests green, end-to-end included | CI, test jobs and Playwright |
| No dependency vulnerability without a valid exception | OSV-Scanner |
| No new CodeQL finding | CodeQL |
| No secret in history | gitleaks |
| No new **high** risk without an owner and deadline in this register | PR review |
| Threat model and ASVS checklist updated if an authentication flow, resource ownership, stored data or the pipeline changed | PR review |

An already registered high risk does not block a release: blocking everything because of R-01 would
hold its own fix back too. What blocks is a **new** high risk that nobody has assessed.

## How the register is maintained

At the scheduled review, the owner confirms the level, adjusts the deadline, or closes the risk with a
link to the PR that fixed it. Closed risks leave the table and stay in git history.
