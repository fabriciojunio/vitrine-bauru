# 2. Transporte de eventos com dois adaptadores

Data: 2026-08-30
Situação: aceita

## Contexto

O sistema é orientado a eventos e o transporte natural é o Kafka. O problema é
outro: este é um projeto de graduação, feito por cinco alunos, e a plataforma
precisa ficar no ar para a SEDECON e para a banca sem custar nada.

Em 2026, o levantamento de opções gratuitas deu isto:

- **Kafka gerenciado**: o Redpanda Cloud tem 14 dias de teste; o Confluent
  Cloud dá crédito promocional. Nenhum dos dois tem camada gratuita
  permanente. O Upstash, que tinha, descontinuou o produto de Kafka.
- **Máquina para rodar o broker**: o Render dá 750 horas de máquina por mês
  para o espaço inteiro, e o serviço hiberna depois de 15 minutos parado. Um
  broker que hiberna não é um broker.
- **Quatro serviços Java**: cada um consumiria a própria fatia das 750 horas e
  teria a própria partida a frio de meio minuto. Abrir a vitrine acordaria
  quatro processos.

Desligar o Kafka e escrever chamada HTTP direta entre os serviços resolveria a
conta e jogaria fora o desenho inteiro: sem evento, não há outbox, não há
inbox, não há projeção, e a separação vira quatro monolitos se chamando.

## Decisão

O transporte virou uma interface com dois adaptadores:

```java
public interface TransporteDeEventos {
    void enviar(String topico, String chave, String carga) throws Exception;
}
```

- `TransporteKafka`: publica no broker. É o padrão, e o que roda no
  docker-compose, no Kubernetes e nos testes de integração.
- `TransporteNoProcesso`: entrega ao mesmo despachante, dentro do processo. É
  o que roda na implantação gratuita, onde os quatro módulos estão no mesmo
  processo e não há broker nenhum.

A escolha é uma variável de ambiente: `TRANSPORTE_DE_EVENTOS=kafka` ou
`=processo`.

## O que muda e o que não muda

Muda o caminho da mensagem entre o publicador e o consumidor. Só isso.

Não muda: o evento continua sendo gravado no outbox na mesma transação do
estado; o publicador continua marcando como publicada só depois da entrega
confirmada; o consumidor continua idempotente pelo inbox; a falha continua
voltando como exceção, com tentativa de novo e espera crescente; e os
consumidores são as mesmas classes, que não sabem qual transporte está em uso.

## Consequências

**O que se perde sem broker.** Não há retenção além do processo: se ele cair
com evento no outbox, a entrega acontece na volta, mas nada fica bufferizado
fora dali. Não há consumo em paralelo por partição. Não há fila morta, e o que
faz o papel dela é a mensagem parada no outbox depois de dez tentativas, que
vira métrica e log de erro. E a entrega é síncrona: o publicador espera o
consumidor terminar.

**O que se ganha.** A demonstração fica no ar de graça, com o mesmo código, e
a volta para quatro processos com Kafka é trocar uma variável de ambiente.

**O risco assumido.** Duas formas de implantação significam dois caminhos para
testar. Por isso a suíte cobre os dois: os testes de cada serviço rodam com
Kafka embutido, e o teste de jornada completa roda com o transporte no
processo, exatamente como a demonstração publicada.

## Alternativas consideradas

**Pagar por um Kafka gerenciado.** Custa entre 20 e 50 dólares por mês. Não há
orçamento, e pedir cartão de crédito a um aluno para manter um projeto de
disciplina no ar não é razoável.

**RabbitMQ no CloudAMQP.** Tem camada gratuita permanente, com limite de 100
conexões e 1 milhão de mensagens por mês, o que caberia. Foi recusado porque
trocaria Kafka por outro produto no currículo do projeto sem resolver o
problema real, que é o número de processos, e porque o CloudAMQP também some
com a instância gratuita depois de 30 dias sem uso.

**Banco como fila.** Fazer o outbox ser lido diretamente pelos consumidores,
sem transporte nenhum. É essencialmente o que o adaptador no processo faz, mas
sem a interface no meio, o que impediria a volta para o Kafka.
