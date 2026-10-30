# Análise de ameaças: fluxo de dados, árvore de ataque, PASTA e DREAD

*[Read this in English](analise-de-ameacas.en.md)*

Revisado em 29/10/2026. Complementa o [modelo STRIDE](modelo-de-ameacas.md): lá está cada ameaça
com o controle e o teste que o prova; aqui está o cenário mais grave visto de três ângulos, a
pontuação para priorizar e os requisitos que saem disso.

## 1. Diagrama de fluxo de dados

```
                 ┌────────────────────── FC1: internet ──────────────────────┐
  [consumidor]   │                                                            │
  [empreendedor] ├─HTTPS─> (1) front React ──HTTPS + Bearer──> (2) API Spring │
  [moderador]    │         Vercel, CSP                        Render          │
                 └────────────────────────────────────────────────┬──────────┘
                                                                   │
                 ┌──────────── FC2: dados ─────────────┐           │
                 │  [D1] PostgreSQL gerenciado ◄──TLS──┼───────────┤
                 │   contas, perfis, catálogo, fotos,  │           │
                 │   auditoria, outbox/inbox           │           │
                 └─────────────────────────────────────┘           │
                 ┌──────────── FC3: terceiros ─────────┐           │
                 │  (3) Resend: e-mail  ◄──HTTPS───────┼───────────┤
                 │  (4) BrasilAPI: CNPJ ◄──HTTPS───────┼───────────┘
                 │  (5) ViaCEP: CEP ◄──HTTPS── direto do front (1)
                 └─────────────────────────────────────┘
                 ┌──────────── FC4: entrega ───────────┐
                 │  GitHub Actions: senha do banco da demonstração,
                 │  Deploy Hook do Render, deploy da Vercel
                 └─────────────────────────────────────┘
```

| Fluxo | Dado | Quem autentica quem |
|---|---|---|
| Navegador → front | nada sensível; HTML e JS estáticos | TLS da Vercel |
| Front → API | credencial no login; depois token de acesso (15 min) e de renovação | TLS do Render; API valida o HMAC do token |
| API → banco | tudo, inclusive documento, contato e hash de senha | TLS e usuário do banco |
| API → Resend | destinatário e corpo do e-mail | chave da API do Resend |
| API → BrasilAPI | CNPJ informado no cadastro | nenhuma (API pública) |
| Navegador → ViaCEP | CEP digitado no cadastro, direto do front | nenhuma (API pública); liberado no `connect-src` da CSP |
| CI → Render, Neon | código, senha do banco da demonstração | segredos do GitHub |

## 2. Cenário mais grave e árvore de ataque

**Objetivo do atacante:** obter os documentos e contatos de todos os empreendedores, ou aprovar
lojas falsas para golpe com o selo da prefeitura. Os dois passam pelo mesmo ativo: uma sessão de
moderador (`ADMIN_SEDECON`).

```
OBJETIVO: agir como moderador da SEDECON
├── OU 1. Roubar a credencial do moderador
│   ├── 1.1 Força bruta no login ............... bloqueio em 5 erros + limite por endereço [mitigado]
│   ├── 1.2 Credencial vazada de outro site .... sem segundo fator [ABERTO, R-01]
│   └── 1.3 Phishing da tela de login .......... sem segundo fator resistente a phishing [ABERTO, R-01]
├── OU 2. Roubar o token já emitido
│   ├── 2.1 XSS no front lendo o localStorage .. CSP restrita + escape do React + jsoup [mitigado]
│   │   └── E 2.1.1 dependência do front comprometida  OSV + Dependabot 7 dias [mitigado, residual]
│   └── 2.2 Token em log ou URL ................ token só no cabeçalho; log sem cabeçalho [mitigado]
├── OU 3. Forjar ou elevar o token
│   ├── 3.1 Forjar HMAC ........................ segredo de 32+ bytes validado na subida [mitigado]
│   ├── 3.2 Papel vindo do corpo da requisição . papel só do token [mitigado]
│   └── 3.3 Empreendedor chamando rota de moderação  hasRole por rota + teste [mitigado]
└── OU 4. Atalhar a aplicação
    ├── 4.1 Senha do banco no pipeline ......... segredo do GitHub, permissions mínimas [mitigado]
    │   └── E 4.1.1 action comprometida ........ actions por SHA [mitigado]
    └── 4.2 Ler o banco por SQL injection ...... JPA com parâmetro + teste de carga [mitigado]
```

Os dois ramos abertos são o mesmo requisito: segundo fator para o moderador (R-01).

## 3. PASTA, uma rodada

| Estágio | O que saiu |
|---|---|
| 1. Objetivos do negócio | Dar visibilidade a empreendedor local com aval da SEDECON; o aval só vale se a moderação for confiável; dado pessoal tratado conforme a LGPD |
| 2. Escopo técnico | Front React (Vercel), API Spring Boot 3 num processo (Render), PostgreSQL gerenciado, Resend, BrasilAPI, GitHub Actions |
| 3. Decomposição | Seção 1: quatro fronteiras, seis fluxos, ativos em [modelo-de-ameacas.md](modelo-de-ameacas.md) |
| 4. Ameaças | Fraude por loja falsa aprovada; coleta em massa de contatos para spam ou golpe; derrubada da vitrine pública na véspera de feira |
| 5. Vulnerabilidades | Sem segundo fator (R-01); limite de requisição por instância (R-04); log só no Render (R-03); token de renovação no `localStorage` (ADR 0010) |
| 6. Ataque | Árvore da seção 2; o caminho mais barato é credencial reaproveitada do moderador (1.2) |
| 7. Risco e impacto | Selo da prefeitura em loja golpista: dano à reputação da SEDECON e ao munícipe; vazamento de CPF e telefone: incidente comunicável à ANPD |

## 4. DREAD

Escala de 1 a 3 por critério (dano, reprodutibilidade, explorabilidade, usuários afetados,
descoberta). Usada só para ordenar; quem decide o prazo é o [registro de riscos](riscos.md).

| Ameaça | D | R | E | A | D | Total | Situação |
|---|---|---|---|---|---|---|---|
| Credencial do moderador reaproveitada (1.2) | 3 | 3 | 2 | 3 | 2 | 13 | aberta, R-01 |
| Coleta em massa da vitrine pública por robô | 2 | 3 | 3 | 2 | 3 | 13 | parcial: limite por endereço, R-04 |
| XSS com dependência comprometida (2.1.1) | 3 | 1 | 1 | 3 | 1 | 9 | residual |
| Moderador apaga rastro no log da aplicação | 2 | 2 | 1 | 1 | 1 | 7 | aberta, R-03 |
| Forjar token (3.1) | 3 | 1 | 1 | 3 | 1 | 9 | mitigada |
| Derrubar a API por volume | 2 | 3 | 2 | 3 | 3 | 13 | parcial: limite por instância e Render, R-04 |

DREAD tem fama de subjetivo, e é: o "descoberta" pune o que é óbvio, e o que é óbvio deveria estar
corrigido. Fica aqui porque o documento de carreira pede e porque algumas empresas ainda usam; para
decidir, o CVSS e o registro de riscos são melhores.

## 5. Requisitos de segurança derivados

| Id | Requisito | Ameaça | Situação | Verificação |
|---|---|---|---|---|
| RS-01 | Moderador entra com segundo fator (TOTP ou passkey) | 1.2, 1.3 | pendente (R-01) | teste de login sem segundo fator recusado |
| RS-02 | Toda rota de moderação exige `ADMIN_SEDECON` no servidor | 3.3 | atendido | `SegurancaHttpTest` |
| RS-03 | Dono do recurso vem do token, nunca da URL ou do corpo | 3.2 | atendido | `FluxoDoCatalogoTest` |
| RS-04 | Ação de moderação registrada com autor, data e correlação, e o registro sobrevive à exclusão do titular | repúdio | atendido | `FluxoDoCadastroTest.auditoriaRegistra`, `JornadaCompletaTest` (conta anonimizada, auditoria mantida) |
| RS-05 | Log de auditoria copiado para destino que a conta da aplicação não apaga | moderador apaga rastro | pendente (R-03) | |
| RS-06 | Limite de requisição compartilhado entre instâncias | coleta em massa, DoS | pendente (R-04) | |
| RS-07 | CSP sem `unsafe-inline` em script nem em estilo, restrita ao host da API | 2.1 | atendido | `web/vercel.json`, ZAP |
| RS-08 | Dependência nova só após 7 dias e sem falha conhecida | 2.1.1 | atendido | OSV-Scanner, Dependabot |
| RS-09 | Segredo do pipeline com escopo mínimo, actions por SHA | 4.1 | atendido | revisão dos workflows |
| RS-10 | Consulta ao banco só parametrizada | 4.2 | atendido | `SegurancaHttpTest` (carga de injeção) |
