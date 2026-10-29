# Checklist de PR seguro

*[Read this in English](checklist-de-pr.en.md)*

Para quem revisa um PR da Vitrine. Não é para marcar tudo: é para responder às perguntas que se
aplicam ao que mudou. Se uma resposta for "não sei", o PR pede ajuda antes de entrar.

## Mudou uma rota da API

- [ ] Quem pode chamar está na configuração do Spring Security, e não só no front?
- [ ] O dono do recurso sai do token, e não do caminho, da query ou do corpo?
- [ ] Recurso de outra loja é recusado? Se o recurso não é público (conta, documento, pedido de
      exclusão), a resposta é 404, para não confirmar que existe. Se já é público, como o
      produto na vitrine, 403 com mensagem clara é o que o catálogo faz.
- [ ] A entrada é um `record` só com os campos permitidos, com Bean Validation?
- [ ] Há um teste que chama a rota com outro usuário e espera a recusa?

## Mudou dado guardado

- [ ] O dado novo é necessário? Dá para não guardar?
- [ ] Ele aparece em log, em mensagem de erro ou na resposta de outra rota?
- [ ] A saga de exclusão ([ADR 0004](../../adr/0004-saga-de-exclusao-lgpd.md)) apaga o dado novo?
- [ ] O arquivo de "meus dados" inclui o dado novo?

## Mudou texto que o usuário escreve e outro usuário lê

- [ ] Passa pelo sanitizador na entrada?
- [ ] O front mostra como texto, sem `dangerouslySetInnerHTML`?

## Mudou dependência

- [ ] A versão nova tem pelo menos 7 dias de publicada, ou é correção de segurança?
- [ ] O OSV-Scanner passou?
- [ ] A dependência é mantida (último commit, número de mantenedores)?

## Mudou o pipeline

- [ ] Action nova fixada por SHA, com a versão no comentário?
- [ ] `permissions` do job é o mínimo?
- [ ] Algum segredo novo aparece em `echo`, em artefato ou em log?

## Mudou autenticação, dono de recurso, dado guardado ou pipeline

- [ ] O [modelo de ameaças](../modelo-de-ameacas.md) e o [checklist ASVS](../asvs.md) foram
      atualizados no mesmo PR?
- [ ] Algum risco novo entrou no [registro](../riscos.md), com dono e prazo?
