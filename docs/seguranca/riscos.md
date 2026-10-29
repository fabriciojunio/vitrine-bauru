# Registro de riscos, exceções e decisão de release

*[Read this in English](riscos.en.md)*

Revisado em 29/10/2026. Um risco entra aqui quando foi visto e não vai ser corrigido agora. Sem
este registro, "aceito" vira "esquecido".

## Riscos abertos

Probabilidade e impacto de 1 a 3; o nível é o produto (1-2 baixo, 3-4 médio, 6-9 alto).

| Id | Risco | P | I | Nível | Tratamento | Dono | Prazo | Próxima revisão |
|---|---|---|---|---|---|---|---|---|
| R-01 | Moderador sem segundo fator: credencial reaproveitada dá acesso à moderação e aos documentos | 2 | 3 | alto (6) | reduzir: TOTP para `ADMIN_SEDECON` | Fabrício | 31/12/2026 | 30/11/2026 |
| R-02 | Token de renovação no `localStorage`, legível por XSS | 1 | 3 | médio (3) | aceitar, [ADR 0010](../adr/0010-token-de-renovacao-no-armazenamento-local.md); depende da CSP | Fabrício | revisão a cada 6 meses | 29/04/2027 |
| R-03 | Log da aplicação só no Render: quem tem a conta apaga o rastro | 2 | 2 | médio (4) | reduzir: cópia para destino separado e somente anexação | Fabrício | 31/03/2027 | 31/12/2026 |
| R-04 | Limite de requisição em memória: com mais de uma instância, multiplica | 1 | 2 | baixo (2) | aceitar enquanto houver uma instância; reduzir antes de escalar | Fabrício | antes da segunda instância | 29/04/2027 |
| R-05 | `style-src 'unsafe-inline'` no front: `<style>` injetado vaza campo da tela por seletor | 1 | 2 | baixo (2) | reduzir: estilos do React em atributo, `style-src` com hash | Fabrício | 31/03/2027 | 31/12/2026 |
| R-06 | E-mail do usuário dentro do token de acesso, legível por quem tiver o token | 1 | 1 | baixo (1) | aceitar: o token já dá acesso à conta inteira | Fabrício | revisão a cada 6 meses | 29/04/2027 |

## Exceções de ferramenta

Achado de ferramenta que não vai ser corrigido também é risco aceito, só que menor. Elas ficam nos
arquivos da própria ferramenta, sempre com motivo e prazo:

| Onde | O que guarda |
|---|---|
| [`osv-scanner.toml`](../../osv-scanner.toml) | vulnerabilidade de dependência aceita, com `ignoreUntil` |
| `.github/codeql-accepted.json` | alerta do CodeQL aceito, com motivo, mitigação e data de revisão |
| `.github/workflows/dast.yml` e o arquivo de regras do ZAP | alerta do DAST triado como falso positivo |

Exceção sem prazo não é aceita na revisão de código.

## Decisão de release

Uma versão vai de `homologação` para `produção` quando todas as condições abaixo são verdadeiras.
Elas são checadas pela CI, e não pela memória de quem aprova.

| Condição | Onde se confere |
|---|---|
| Testes do back-end e do front verdes, ponta a ponta incluso | CI, jobs de teste e Playwright |
| Nenhuma vulnerabilidade de dependência sem exceção válida | OSV-Scanner |
| Nenhum achado novo do CodeQL | CodeQL |
| Nenhum segredo no histórico | gitleaks |
| Nenhum risco **alto** novo sem dono e prazo neste registro | revisão do PR |
| Modelo de ameaças e checklist ASVS atualizados se mudou fluxo de autenticação, dono de recurso, dado guardado ou pipeline | revisão do PR |

Um risco alto já registrado não bloqueia release: bloquear tudo por causa do R-01 deixaria a
correção dele presa junto. Bloqueia o risco alto **novo**, que ninguém avaliou.

## Como o registro é mantido

Na revisão marcada, o dono confirma o nível, ajusta o prazo ou fecha o risco com o link do PR que
corrigiu. Risco fechado sai da tabela e fica no histórico do git.
