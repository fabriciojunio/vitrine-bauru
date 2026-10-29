# Compliance: LGPD in the Vitrine, and a PCI DSS and BACEN exercise

*[Leia em português](conformidade.md)*

Reviewed on 29/10/2026. Part one is about the real Vitrine. Part two is an exercise with a fictitious
financial system, because the Vitrine handles no cards and is not a financial institution, while
AppSec roles at banks and payment companies ask for both standards.

This is not legal advice. The legal basis and the data protection officer are the controller's
decision, SEDECON's.

## Part 1. LGPD in Vitrine Bauru

### Who and what

| Item | Status |
|---|---|
| Data subjects | registered entrepreneurs; consumers who only browse the showcase have no data stored |
| Data | name, CPF or CNPJ, email, phone, business address, products and photos |
| Sensitive data (art. 5, II) | none |
| Controller | SEDECON, Bauru City Hall |
| Likely legal basis | execution of public policy by the administration (art. 7, III); sign-up is voluntary and the notice says what it is for |
| Data protection officer | to be appointed by the controller |

### Obligation by obligation

| Obligation | How it stands | Proof |
|---|---|---|
| Transparency (art. 6, VI and art. 9) | notice when submitting the sign-up with a link to the privacy page | `web/src/paginas/Cadastrar.tsx`, `Privacidade.tsx` |
| Minimisation (art. 6, III) | consumers have no account, no cookie and no IP in the contact record | `RegistrarContato.java` |
| Access (art. 18, II) | `GET /api/cadastro/privacidade/meus-dados` returns account, business and history | "Meus dados e privacidade" button on the panel |
| Erasure (art. 18, VI) | `DELETE /api/cadastro/privacidade/minha-conta` starts the saga across the four services, with a deadline and alert | [ADR 0004](../adr/0004-saga-de-exclusao-lgpd.md), `JornadaCompletaTest` |
| Security (art. 46) | controls in the [threat model](modelo-de-ameacas.en.md) | tests listed there |
| Record of operations (art. 37) | moderation audit with author and date, kept after erasure | `FluxoDoCadastroTest.auditoriaRegistra` |
| Incident notification (art. 48) | contact in SECURITY.md | **no playbook yet**, see SAMM |

### What is missing

1. Data protection officer and data subject channel appointed by SEDECON.
2. Data protection impact assessment (RIPD): not mandatory for non-sensitive data, but the ANPD may
   ask for one, and a draft is cheap.
3. Retention period for inactive accounts: today data stays until the data subject asks for erasure.
4. Incident playbook with the three-business-day deadline of ANPD Resolution CD/ANPD No. 15/2024.

## Part 2. Exercise: fictitious digital wallet "Pague Leve"

**Simulated data.** Scenario: a payment institution that issues prepaid cards and stores the card
number (PAN) for recurring charges. The controls cited exist in the
[alicerce](https://github.com/fabriciojunio/alicerce) and [brecha](https://github.com/fabriciojunio/brecha)
projects, and the exercise shows where each would fit.

### PCI DSS 4.0.1

| Requirement | What it would demand of Pague Leve | Control I already know how to build |
|---|---|---|
| 1. Network security controls | cardholder data environment (CDE) isolated; only the payment API reaches it | NetworkPolicy and single edge from P2 |
| 2. Secure configurations | no vendor defaults, minimal images | alicerce's non-root distroless image |
| 3. Protect stored account data | PAN encrypted or tokenised; CVV never stored | envelope encryption with KMS, coming in the P3 Terraform |
| 4. Encryption in transit | strong TLS on every public network | TLS at the edge, HSTS |
| 5. Malware protection | image scanning | Trivy in the pipeline |
| 6. Secure systems and software | secure SDLC, review, inventory of payment page scripts (6.4.3) | SAST, SCA, nonce-based CSP (ConectAgente) |
| 7. Access by need to know | deny by default, role per function | alicerce's authorisation |
| 8. Identify and authenticate | MFA for all access into the CDE | AuthCore's MFA |
| 9. Physical access | the cloud provider's | outside the code |
| 10. Logging and monitoring | log every access to card data, stored separately | alicerce's ECS logs with correlation |
| 11. Test regularly | quarterly scans, annual pentest, change detection on payment pages (11.6.1) | ZAP, the brecha report |
| 12. Policy | written policy, training, incident response | alicerce's process, champions |

The most effective way to comply with PCI is **not to store the PAN**: with tokenisation by the
acquirer, most requirements fall out of scope. That is the first recommendation I would make.

### CMN Resolution No. 4,893/2021

Applies to institutions authorised by the Central Bank of Brazil. What it would ask of Pague Leve:

| Requirement | How to meet it |
|---|---|
| Cybersecurity policy approved by the board | document with goals, controls and owners, reviewed every year |
| Procedures to prevent, detect and reduce vulnerability to incidents | alicerce's security pipeline and vulnerability management |
| Action and incident response plan | a playbook like ConectAgente's, adapted to the Central Bank |
| Recording, root cause and impact analysis of relevant incidents | immutable audit trail and a written post-incident report |
| Annual report on the plan's implementation | metrics on time to fix and findings by severity |
| Cloud contracting: supplier assessment and notice to the Central Bank before contracting a relevant service | provider analysis, contract with data access and exit clauses |

### What the exercise shows

All three norms ask for the same thing under different names: know where the data is, control who
reaches it, record who did, and have a plan for when it goes wrong. What changes is the evidence each
auditor accepts.
