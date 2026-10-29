# Como triar um achado do SAST

*[Read this in English](triagem-de-sast.en.md)*

O CodeQL apontou algo no seu PR. Antes de corrigir ou de marcar como falso positivo, quatro
perguntas, nesta ordem.

## 1. O dado vem de quem?

Siga o caminho que a ferramenta mostra, da fonte até o ponto perigoso. Se a fonte é o usuário
(corpo, query, cabeçalho, arquivo enviado), o achado é candidato a real. Se a fonte é constante ou
configuração do servidor, provavelmente não é.

## 2. Algo no caminho já neutraliza?

Validação por lista de permissão, consulta com parâmetro, escape na saída, conversão para número.
A ferramenta nem sempre reconhece o neutralizador. Exemplo real deste repositório (commit
`af73a3d`): o CodeQL não reconheceu `replaceAll("[\\r\\n]", "_")` como limpeza de log, e
reconheceu `replaceAll("\\R", "_")`, que além disso cobre `\r\n` e as quebras Unicode.
A troca foi feita, em vez de silenciar o alerta, porque a próxima pessoa vai ler o mesmo aviso.

## 3. Qual o impacto se for real?

Use o modelo de ameaças: o ponto perigoso toca algum ativo da lista? Documento, token ou hash de
senha é prioridade; texto de produto é menos.

## 4. Decida e registre

| Decisão | O que fazer |
|---|---|
| Real | corrigir no mesmo PR, com um teste que falha sem a correção |
| Real, mas fora do PR | abrir issue com o caminho e a severidade, e registrar no [registro de riscos](../riscos.md) se não for corrigido em 30 dias |
| Falso positivo | registrar em `.github/codeql-accepted.json` com motivo, mitigação, dono e data de revisão, como o aceite do CSRF que já está lá |
| Não sei | marcar o champion no PR; não descartar |

Falso positivo sem motivo escrito volta na revisão. Daqui a seis meses ninguém lembra por que foi
descartado, e o mesmo padrão reaparece em outro lugar.

## Severidade, para a fila

| Severidade do CodeQL | Prazo de correção |
|---|---|
| crítica | 7 dias |
| alta | 30 dias |
| média | 90 dias |
| baixa | na próxima vez que mexer no arquivo |
