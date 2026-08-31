# 6. Busca por texto sem extensão do PostgreSQL

Data: 2026-08-30
Situação: aceita, com gatilho definido para mudar

## Contexto

A busca é a função principal da vitrine, e ela precisa funcionar do jeito que a
pessoa digita no celular: "acai" tem que encontrar "Açaí", "PASTEL" tem que
encontrar "Pastel de feira", e meia palavra tem que encontrar a palavra inteira.

O caminho natural no PostgreSQL é `unaccent` com `pg_trgm` e um índice GIN.
Funciona bem e é o que se usaria num sistema maduro.

Dois problemas apareceram. O primeiro é que `unaccent` não é imutável, então
usá-la numa coluna gerada exige criar uma função de contorno, o que é um truque
conhecido mas que precisa ser explicado a cada pessoa nova no projeto. O
segundo é que nem toda hospedagem gratuita de PostgreSQL habilita extensão, e
descobrir isso no dia da apresentação seria caro.

## Decisão

O texto é normalizado em Java na hora de gravar a projeção: minúsculas, sem
acento, espaços colapsados. O termo digitado passa pela mesma normalização, e a
consulta é um `like` comum sobre a coluna já normalizada.

A mesma função é usada dos dois lados, e existe um teste que prova a
propriedade que interessa: quem procura sem acento encontra quem cadastrou com
acento, e vice-versa.

## Consequências

**O que se ganha.** Roda em qualquer PostgreSQL, sem extensão, sem função de
contorno e sem migração especial. A regra de normalização fica num lugar só, em
código testável, em vez de espalhada entre SQL e aplicação.

**O que se perde.** O `like '%termo%'` não usa índice, então a consulta varre a
tabela. Com algumas centenas de lojas e alguns milhares de produtos, isso são
poucos milissegundos, e a projeção é justamente a tabela mais enxuta do
sistema. Também não há ranking por relevância: os resultados vêm por data, do
mais recente para o mais antigo.

**Consequência aceita e visível num teste.** Termo curto casa demais: procurar
"aca" encontra o açaí e também qualquer produto da categoria "alimentacao",
porque a palavra está no texto de busca. O teste da busca reflete isso, em vez
de fingir que a primeira posição é sempre a esperada.

## Quando mudar

Quando a busca passar de 200 ms com dados reais, ou quando o número de produtos
passar de dez mil. A mudança é criar a extensão e um índice GIN sobre a mesma
coluna `busca`, sem alterar nenhuma linha de código da aplicação:

```sql
create extension if not exists pg_trgm;
create index idx_produto_busca on busca.produto using gin (busca gin_trgm_ops);
```
