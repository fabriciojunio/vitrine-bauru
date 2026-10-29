# OWASP SAMM 2.0 assessment

*[Leia em português](samm.md)*

Assessed on 29/10/2026 by Fabrício Júnio, over the Vitrine Bauru repository and operations. SAMM
scale: 0 (not done), 1 (done ad hoc), 2 (done in a structured way), 3 (done, measured and improved).
Half a point when half of the next level's criteria are met.

The assessment was made by the person who builds the system, which is a bias: SAMM recommends an
independent assessor. The score is a starting point, not a certificate.

## Result

| Function | Practice | Current | Target (Jun/2027) | Evidence |
|---|---|---|---|---|
| Governance | Strategy and metrics | 0.5 | 1 | no written security goals; there is a [risk register](riscos.en.md) and a monthly [evidence report](../../.github/workflows/evidencias.yml) |
| Governance | Policy and compliance | 1 | 1.5 | [LGPD compliance](conformidade.en.md), SECURITY.md |
| Governance | Education and guidance | 1 | 2 | [Security Champions kit](champions/), CONTRIBUTING.md |
| Design | Threat assessment | 2 | 2 | [STRIDE](modelo-de-ameacas.en.md) maintained per PR, [PASTA and DREAD analysis](analise-de-ameacas.en.md), [ATT&CK](attack.en.md) |
| Design | Security requirements | 1.5 | 2 | [ASVS checklist](asvs.en.md), requirements RS-01 to RS-10 |
| Design | Security architecture | 1.5 | 2 | ADRs 0001 to 0010; no catalogue of reusable patterns yet |
| Implementation | Secure build | 2 | 2.5 | actions pinned by SHA, pinned dependencies, SBOM at build, Dependabot with 7 days |
| Implementation | Secure deployment | 1.5 | 2 | back-end deployed only after green CI on `produção` (Render auto-deploy off); secrets in GitHub and Render; no artifact signing yet |
| Implementation | Defect management | 1 | 2 | OSV with `ignoreUntil`, CodeQL acceptances with review date, time to fix in the monthly report; no dashboard yet |
| Verification | Architecture assessment | 1 | 1.5 | review against the threat model in each PR |
| Verification | Requirements-driven testing | 2 | 2.5 | `SegurancaHttpTest` and flow tests prove each control |
| Verification | Security testing | 2 | 2.5 | CodeQL, OSV, gitleaks and tests required before merging into `produção`; ZAP against the published site after deploy |
| Operations | Incident management | 0.5 | 1.5 | contact in SECURITY.md and [disclosure policy](divulgacao.en.md); no incident playbook yet |
| Operations | Environment management | 1 | 1.5 | headers and CSP checked by ZAP; no configuration check of Render and the database yet |
| Operations | Operational management | 0.5 | 1.5 | no own backup: relies on what the database provider offers on the free plan, which has not been tested; no retention defined |

Current average: **1.3**. Target: **1.9**.

## Plan to reach the target

| Deadline | Action | Practices raised |
|---|---|---|
| Dec/2026 | Second factor for moderators (RS-01) | Requirements, Deployment |
| Dec/2026 | Personal data incident playbook, along the lines of ConectAgente's | Incidents |
| Mar/2027 | Audit log in a separate destination (RS-05) | Environment, Operational |
| Mar/2027 | Findings dashboard with mean time to fix, fed by CI | Defects, Metrics |
| Mar/2027 | Back-end image signed with cosign and verified at deploy | Deployment |
| Jun/2027 | Database restore test, with measured time | Operational |
| Jun/2027 | Champions training given to someone outside the project | Education |

## What deliberately does not change the score

Having a tool is not having a process. ZAP running with nobody looking at the result would be level 1
in security testing; it counts as 2 because it fails CI and every exception has a written reason,
and the other security tests are required before merging.
