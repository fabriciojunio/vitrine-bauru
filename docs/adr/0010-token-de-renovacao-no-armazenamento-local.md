# 10. Token de renovação no armazenamento local do navegador

Data: 2026-10-29
Situação: aceita, com revisão a cada seis meses (risco R-02)

## Contexto

O empreendedor volta ao painel dias depois de entrar. Sem guardar algo no navegador, ele digita a
senha toda vez, e painel que pede senha toda vez é painel que o empreendedor deixa de usar.

O token de acesso dura 15 minutos e fica só em memória. O que precisa sobreviver ao fechamento da
aba é o token de renovação, que dura dias e é trocado a cada uso.

O front está na Vercel (`vitrine-bauru.vercel.app`) e a API no Render
(`vitrine-bauru-api.onrender.com`): domínios diferentes, sem domínio próprio para unir os dois.

## Alternativas

| Opção | O que resolve | O que custa |
|---|---|---|
| A. `localStorage` | simples; sem CSRF, porque o token vai no cabeçalho | qualquer XSS no front lê o token |
| B. Cookie `HttpOnly` da API | XSS não lê o token | domínios diferentes exigem `SameSite=None`, e aí o navegador manda o cookie em requisição de outro site: volta o CSRF, que hoje não existe |
| C. Cookie `HttpOnly` com BFF no mesmo domínio do front | XSS não lê, e `SameSite=Strict` funciona | um servidor a mais para manter e um salto a mais em cada chamada |
| D. Domínio próprio, front e API como subdomínios | cookie `SameSite=Lax` no domínio pai | o projeto não tem domínio próprio; registrar um custa por ano |

## Decisão

Opção A. O token de renovação fica no `localStorage`, guardado como resumo SHA-256 no servidor,
rotacionado a cada uso, e o reuso de um token já trocado derruba todas as sessões da conta
(`FluxoDoCadastroTest`).

O que sustenta a decisão é a defesa contra XSS, e ela tem três camadas: CSP sem script embutido no
front e `default-src 'none'` na API; escape do React na saída; jsoup na entrada da descrição.

## Onde discordo da escolha comum

O conselho mais repetido é "nunca guarde token no `localStorage`, use cookie `HttpOnly`". Ele
ignora que, com domínios diferentes, o cookie vira `SameSite=None` e troca uma classe de falha
(XSS lendo o token) por outra (CSRF usando o token), sem a vantagem de ter eliminado a primeira:
um XSS que não lê o cookie ainda faz requisição autenticada a partir da página. O que de fato
reduz o risco é não ter XSS, e é aí que o esforço foi posto.

## Quando revisar

- Se o projeto ganhar domínio próprio, ou um subdomínio da prefeitura: opção D.
- Se aparecer um XSS, mesmo corrigido: reavaliar a opção C.
- Se a CSP do front precisar de `unsafe-inline` em script por algum motivo: a decisão cai.
