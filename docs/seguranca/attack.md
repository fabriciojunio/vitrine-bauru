# Técnicas do MITRE ATT&CK que se aplicam à Vitrine

*[Read this in English](attack.en.md)*

Revisado em 29/10/2026, matriz Enterprise. Só entra técnica que tem caminho real neste sistema; as
de estação de trabalho e de rede corporativa ficam de fora, porque aqui não há nenhuma das duas.

A coluna **detecção** é a que costuma faltar em projeto de portfólio, e aqui também falta em
parte: prevenir está bem coberto, perceber que alguém tentou ainda não.

| Tática | Técnica | Como seria aqui | Prevenção | Detecção |
|---|---|---|---|---|
| Reconhecimento | T1595.002 Varredura de vulnerabilidade | robô varrendo a API e o front | cabeçalhos, Problem Details sem detalhe, actuator só com papel | log de 4xx por endereço; sem alerta (lacuna) |
| Reconhecimento | T1589 Coleta de identidade da vítima | baixar a vitrine pública para juntar nome e telefone de empreendedor | contato exibido só o que o empreendedor publicou; limite na busca e no contato | contagem de contato por endereço no log; sem alerta (lacuna) |
| Acesso inicial | T1190 Exploração de aplicação exposta | injeção, XSS, upload malicioso | JPA parametrizado, jsoup, tipo de imagem pelos bytes, CSP | CodeQL e ZAP antes do deploy; em produção, só o log de erro |
| Acesso inicial | T1078 Contas válidas | entrar com credencial vazada do moderador | bloqueio após 5 erros; **sem segundo fator (R-01)** | auditoria de cada ação de moderação com autor |
| Acesso inicial | T1566.002 Phishing com link | página falsa de login da SEDECON | nenhuma técnica: o que resolve é passkey (R-01) | nenhuma |
| Acesso inicial | T1195.001 Comprometimento de dependência | pacote npm ou Maven trocado | dependência fixada, 7 dias de espera, OSV, SBOM | OSV a cada push e Dependabot |
| Acesso inicial | T1195.002 Comprometimento da cadeia de software | action do GitHub trocada | actions por SHA, `permissions` mínimo | revisão do PR do Dependabot |
| Acesso a credencial | T1110.001, .003, .004 Força bruta, password spraying, credential stuffing | tentativa em massa no login | bloqueio por conta e limite por endereço; mesma resposta para conta inexistente | log de bloqueio; sem alerta (lacuna) |
| Acesso a credencial | T1528 Roubo de token de aplicação | XSS lendo o token de renovação | CSP, escape, [ADR 0010](../adr/0010-token-de-renovacao-no-armazenamento-local.md); reuso derruba as sessões | reuso de token detectado, gravado na auditoria e no log |
| Acesso a credencial | T1552 Credencial desprotegida | senha do banco no repositório ou no log | segredos só no GitHub e no Render; gitleaks no histórico | gitleaks bloqueia o push |
| Persistência | T1098 Manipulação de conta | moderador comprometido reativa conta suspensa | ação exige `ADMIN_SEDECON` e fica na auditoria | auditoria; sem revisão periódica (lacuna) |
| Coleta | T1213 Dados de repositório de informação | moderador baixando documento de todos | documento mascarado na tela; acesso só por papel | auditoria registra moderação, não leitura (lacuna) |
| Exfiltração | T1567 Exfiltração por serviço web | dado levado por script injetado | `connect-src` só para a API e o ViaCEP | violação de CSP não é reportada (lacuna) |
| Impacto | T1565.001 Manipulação de dado armazenado | aprovar loja falsa | moderação com papel e auditoria | auditoria |
| Impacto | T1499.003 Esgotamento da aplicação | busca pesada em volume | limite por endereço (R-04), busca sem extensão do banco ([ADR 0006](../adr/0006-busca-sem-extensao-do-banco.md)) | métrica de tempo de resposta |
| Evasão | T1070 Remoção de indicador | apagar log da aplicação no Render | **nenhuma (R-03)** | nenhuma |

## Lacunas de detecção, em ordem

1. Alerta para tentativa de login em massa e para bloqueio de conta de moderador.
2. Registro de leitura de documento pelo moderador, não só de decisão.
3. Relatório de violação de CSP (`report-to`), para saber de XSS tentado.
4. Log em destino que a conta da aplicação não apaga (R-03).

Cada uma vira regra Sigma quando o log sair do Render, porque regra sem log para ler é só texto.
