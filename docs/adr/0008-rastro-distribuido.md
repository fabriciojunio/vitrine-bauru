# 8. Rastro distribuído que atravessa o outbox

Data: 2026-09-02
Situação: aceita

## Contexto

A pergunta que o sistema não sabia responder: um cadastro foi aprovado, o empreendedor demorou
para aparecer na busca, e onde foi o tempo?

O que existia antes: métrica por serviço, com Micrometer e Prometheus, e um identificador de
correlação carregado no MDC do log. Isso responde "está lento" e permite juntar as linhas de log
de um mesmo pedido, se você souber procurar. Não responde "está lento onde", e a diferença entre
as duas coisas é meia hora de investigação.

Num sistema em que o pedido atravessa quatro serviços por evento, essa é a primeira pergunta de
qualquer plantão. Também é a primeira pergunta de qualquer entrevista sobre microsserviços, e
"log com correlação" é a resposta que denuncia que o sistema nunca foi investigado sob pressão.

## O problema de verdade não é ligar o rastreamento

Ligar Micrometer Tracing no Spring Boot é uma dependência e três linhas de configuração. O
rastro de HTTP funciona sozinho.

O problema é o outbox, e ele é específico deste desenho.

O evento é gravado dentro da transação de quem atendeu a requisição. É publicado depois, por
uma tarefa agendada, em outra thread, quando aquela requisição já terminou e o contexto dela já
foi descartado. O contexto de rastro vive na thread.

Sem tratar isso, o painel mostra dois rastros que não se conhecem: um que começa no `POST` e
termina no commit, e outro que começa do nada, meio segundo depois, na publicação. O caso que
interessa investigar, que é o pedido do usuário atravessando o sistema, é exatamente o que se
perde.

É a mesma armadilha que o outbox resolve para a entrega, aparecendo de novo na observabilidade:
o que atravessa transação precisa ser gravado, não guardado em memória.

## Decisão

O contexto viaja junto com a mensagem, do mesmo jeito que a carga.

**No outbox, numa coluna.** `trace_pai`, no formato `traceparent` do W3C, gravado na mesma
linha e na mesma transação do evento. Aceita nulo porque nem todo evento nasce de requisição: os
de tarefa agendada não têm rastro anterior para herdar, e isso é normal e não erro.

**Na rede, em cabeçalho.** O `TransporteKafka` põe no cabeçalho do registro, o `TransporteSns`
põe em atributo de mensagem. Nos dois casos fora da carga, porque a carga é o contrato entre
serviços: quem consome um evento não deveria precisar desserializar dado de observabilidade para
entender o que aconteceu.

**Formato do W3C, e não invenção nossa.** É o mesmo `traceparent` que viaja em cabeçalho HTTP.
Guardar o padrão é o que permite o rastro atravessar processo, broker e serviço de terceiro sem
tradução no meio, e é o que faz o painel ligar as pontas sem configuração extra.

**Trecho filho, e não continuação do mesmo.** Publicar e consumir são trabalhos separados, que
acontecem em momentos diferentes e podem falhar independentemente. O painel precisa mostrar os
dois com duração própria, pendurados no mesmo pedido de origem.

O código todo mora em `RastroDaMensagem`, com dois métodos: `capturar`, chamado na gravação, e
`consumindo`, que envolve o despacho. Os dois ouvintes usam o mesmo, e o que muda entre Kafka e
SQS é só de onde o `traceparent` é lido.

## Desligado por padrão

`management.tracing.enabled` vem `false`, e ligar exige apontar o endereço do coletor.

Não é medo de custo, é do log: exportador sem destino tenta conectar, falha e reclama, num laço,
para sempre. Em implantação gratuita, que é onde a demonstração roda, não há coletor. Deixar
ligado por padrão trocaria uma pergunta sem resposta por um log inútil.

A amostragem vem em 100% para desenvolvimento. Em produção com volume, 10% já responde "onde
demorou" sem multiplicar por dez a conta de armazenamento.

## O que se ganha e o que não

Ganha-se a linha do tempo: requisição, gravação, publicação, consumo em cada serviço, com
duração de cada trecho e o erro marcado onde aconteceu.

Não se ganha profiling. O rastro diz que o consumo do catálogo levou 800 ms; não diz qual
consulta dentro dele. Para isso seria preciso instrumentar o repositório, e não foi feito porque
o custo de manutenção não se paga num sistema deste tamanho.

Também não substitui a métrica. Rastro amostrado responde sobre um pedido; métrica responde
sobre todos. Quem tem só rastro descobre que o caso que ficou de fora da amostra era o que
importava.

## Testes

O teste usa o OpenTelemetry de verdade, com exportador em memória, e não um dublê. A razão é
direta: o que precisa ser provado é o formato do W3C, e um dublê aceitaria qualquer texto como
contexto. O teste passaria com uma propagação que não funciona em lugar nenhum.

Os dois que sustentam a decisão são "a publicação continua o rastro da requisição" e "o consumo
continua o rastro do publicador", os dois conferindo igualdade de identificador do rastro entre
as pontas. Se um deles cair, o outbox voltou a cortar o rastro no meio.
