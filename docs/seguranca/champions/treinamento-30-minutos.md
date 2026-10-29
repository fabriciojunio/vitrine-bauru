# Treinamento de 30 minutos: as três falhas que mais aparecem num sistema como a Vitrine

*[Read this in English](treinamento-30-minutos.en.md)*

Para quem entra no projeto. Cada bloco é um slide ou dois; os exemplos são do próprio código.

## Antes (2 minutos)

Pergunta para abrir: "se você tentar editar o produto de outra loja trocando o número na
requisição, o que deveria acontecer?" Deixar responderem. A maioria responde "o front não deixa".
A resposta certa é "o servidor recusa", porque o front não é barreira: qualquer um chama a API
direto.

## 1. Controle de acesso quebrado (10 minutos)

- É a primeira do OWASP Top 10 e da API Security Top 10, com nomes diferentes (BOLA, IDOR).
- Exemplo errado, escrito no quadro: `GET /lojas/{id}/produtos` buscando por `id` sem conferir
  dono.
- Como a Vitrine faz: o dono sai do token, e produto de outra loja recebe 403
  (`FluxoDoCatalogoTest`, "não deixa mexer no produto de outra loja"). Mostrar o teste.
- 403 ou 404? Produto já é público, então 403 não revela nada. Para recurso que não é público,
  404, para não confirmar que existe.
- Exercício (3 minutos): o que está errado aqui?

  ```java
  @PutMapping("/produtos/{id}")
  ProdutoResposta alterar(@PathVariable UUID id, @RequestBody AlteracaoDeProduto corpo) {
      return catalogo.alterar(id, corpo.lojaId(), corpo.nome(), corpo.preco());
  }
  ```

  Resposta: a loja dona vem do corpo (`corpo.lojaId()`), que quem chama escreve como quiser. Tem
  que vir do token, como em `FluxoDoCatalogoTest`.

## 2. Injeção e XSS (8 minutos)

- Injeção: JPA com parâmetro. Mostrar a carga de injeção no `SegurancaHttpTest`.
- XSS: o empreendedor escreve a descrição, o consumidor lê. jsoup na entrada, escape do React na
  saída, CSP como última barreira.
- Por que a CSP importa aqui: o token de renovação está no `localStorage`
  ([ADR 0010](../../adr/0010-token-de-renovacao-no-armazenamento-local.md)).

## 3. Dado pessoal onde não devia (6 minutos)

- Log com CPF, e-mail no corpo do log, erro com pilha para o cliente.
- Como a Vitrine faz: documento mascarado, Problem Details genérico com correlação.
- Exercício (2 minutos): o que há de errado em `log.info("Cadastro de {}", requisicao)`?
  Resposta: o `toString` do record leva o CPF.

## Fechamento (4 minutos)

- O [checklist de PR](checklist-de-pr.md) e onde ele fica.
- Achou algo estranho? Não é vergonha perguntar: abra o PR como rascunho e marque o champion.
- Pergunta final: repetir a do início. Agora todo mundo responde "o servidor recusa, e há um
  teste provando".

## Como saber se funcionou

Duas semanas depois, olhar os PRs de quem fez o treinamento: algum teste de acesso com outro
usuário apareceu sem ninguém pedir?
