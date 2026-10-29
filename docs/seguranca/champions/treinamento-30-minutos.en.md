# 30-minute training: the three flaws that show up most in a system like the Vitrine

*[Leia em português](treinamento-30-minutos.md)*

For newcomers to the project. Each block is one or two slides; the examples come from the code itself.

## Before (2 minutes)

Opening question: "if you try to edit another shop's product by changing the number in the request,
what should happen?" Let people answer. Most say "the front won't let you". The right answer is "the
server refuses", because the front is not a barrier: anyone can call the API directly.

## 1. Broken access control (10 minutes)

- First in the OWASP Top 10 and in the API Security Top 10, under different names (BOLA, IDOR).
- Wrong example, written on the board: `GET /lojas/{id}/produtos` fetching by `id` without checking
  the owner.
- How the Vitrine does it: the owner comes from the token, and another shop's product gets 403
  (`FluxoDoCatalogoTest`, "não deixa mexer no produto de outra loja"). Show the test.
- 403 or 404? A product is already public, so 403 reveals nothing. For a resource that is not public,
  404, so as not to confirm it exists.
- Exercise (3 minutes): what is wrong here?

  ```java
  @PutMapping("/produtos/{id}")
  ProdutoResposta alterar(@PathVariable UUID id, @RequestBody AlteracaoDeProduto corpo) {
      return catalogo.alterar(id, corpo.lojaId(), corpo.nome(), corpo.preco());
  }
  ```

  Answer: the owning shop comes from the body (`corpo.lojaId()`), which the caller can write as they
  please. It must come from the token, as in `FluxoDoCatalogoTest`.

## 2. Injection and XSS (8 minutes)

- Injection: JPA with parameters. Show the injection payload in `SegurancaHttpTest`.
- XSS: the entrepreneur writes the description, the consumer reads it. jsoup on input, React escaping
  on output, CSP as the last barrier.
- Why the CSP matters here: the refresh token is in `localStorage`
  ([ADR 0010](../../adr/0010-token-de-renovacao-no-armazenamento-local.md)).

## 3. Personal data where it should not be (6 minutes)

- Logs with CPF, email bodies in logs, stack traces sent to the client.
- How the Vitrine does it: masked document, generic Problem Details with correlation.
- Exercise (2 minutes): what is wrong with `log.info("Cadastro de {}", requisicao)`? Answer: the
  record's `toString` carries the CPF.

## Wrap-up (4 minutes)

- The [PR checklist](checklist-de-pr.en.md) and where it lives.
- Found something odd? There is no shame in asking: open the PR as a draft and tag the champion.
- Final question: repeat the opening one. Now everyone answers "the server refuses, and there is a
  test proving it".

## How to know it worked

Two weeks later, look at the PRs of whoever took the training: did an access test with another user
show up without anyone asking?
