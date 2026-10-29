# MITRE ATT&CK techniques that apply to the Vitrine

*[Leia em português](attack.md)*

Reviewed on 29/10/2026, Enterprise matrix. Only techniques with a real path in this system are
listed; workstation and corporate network techniques are left out, because there is neither here.

The **detection** column is the one usually missing from portfolio projects, and it is partly missing
here too: prevention is well covered, noticing that someone tried is not yet.

| Tactic | Technique | How it would happen here | Prevention | Detection |
|---|---|---|---|---|
| Reconnaissance | T1595.002 Vulnerability scanning | a bot scanning the API and the front | headers, Problem Details without detail, actuator only with a role | log of 4xx per address; no alert (gap) |
| Reconnaissance | T1589 Gather victim identity information | downloading the public showcase to collect entrepreneurs' names and phones | only what the entrepreneur published is shown; limits on search and contact | contact count per address in the log; no alert (gap) |
| Initial access | T1190 Exploit public-facing application | injection, XSS, malicious upload | parameterised JPA, jsoup, image type from bytes, CSP | CodeQL and ZAP before release; in production, only the error log |
| Initial access | T1078 Valid accounts | signing in with the moderator's leaked credentials | lockout after 5 errors; **no second factor (R-01)** | audit of each moderation action with its author |
| Initial access | T1566.002 Spearphishing link | fake SEDECON login page | no technical control: passkeys fix it (R-01) | none |
| Initial access | T1195.001 Compromise software dependencies | swapped npm or Maven package | pinned dependencies, 7-day wait, OSV, SBOM | OSV on every push and Dependabot |
| Initial access | T1195.002 Compromise software supply chain | swapped GitHub action | actions pinned by SHA, minimal `permissions` | review of the Dependabot PR |
| Credential access | T1110.001, .003, .004 Brute force, password spraying, credential stuffing | mass login attempts | per-account lockout and per-address limit; same response for unknown accounts | lockout log; no alert (gap) |
| Credential access | T1528 Steal application access token | XSS reading the refresh token | CSP, escaping, [ADR 0010](../adr/0010-token-de-renovacao-no-armazenamento-local.md); reuse revokes all sessions | token reuse detected, written to the audit trail and the log |
| Credential access | T1552 Unsecured credentials | database password in the repository or in a log | secrets only in GitHub and Render; gitleaks over history | gitleaks blocks the push |
| Persistence | T1098 Account manipulation | compromised moderator reactivates a suspended account | action requires `ADMIN_SEDECON` and is audited | audit; no periodic review (gap) |
| Collection | T1213 Data from information repositories | moderator downloading everyone's documents | document masked on screen; access only by role | audit records moderation decisions, not reads (gap) |
| Exfiltration | T1567 Exfiltration over web service | data taken by an injected script | `connect-src` only to the API and ViaCEP | CSP violations are not reported (gap) |
| Impact | T1565.001 Stored data manipulation | approving a fake shop | moderation with role and audit | audit |
| Impact | T1499.003 Application exhaustion flood | heavy searches at volume | per-address limit (R-04), search without database extensions ([ADR 0006](../adr/0006-busca-sem-extensao-do-banco.md)) | response time metric |
| Defence evasion | T1070 Indicator removal | deleting the application log on Render | **none (R-03)** | none |

## Detection gaps, in order

1. Alerts for mass login attempts and for moderator account lockouts.
2. Recording when a moderator reads a document, not only decisions.
3. CSP violation reports (`report-to`), to learn about attempted XSS.
4. Logs in a destination the application account cannot delete (R-03).

Each becomes a Sigma rule once the log leaves Render, because a rule with no log to read is just text.
