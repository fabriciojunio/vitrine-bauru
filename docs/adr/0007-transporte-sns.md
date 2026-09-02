# 7. Transporte gerenciado com SNS e SQS

Data: 2026-09-02
Situação: aceita
Corrige uma premissa da [0002](0002-transporte-de-eventos.md), que continua valendo no resto.

## Contexto

A decisão 0002 diz, com estas palavras, que não havia mensageria gerenciada com camada
gratuita permanente em 2026. A conclusão estava errada, e o motivo do erro interessa mais que
o erro: a busca foi por *Kafka gerenciado*, não pelo problema.

O problema era "como um serviço entrega evento para outro sem eu manter servidor". Procurar
pelo nome da ferramenta descartou de saída todas as respostas que não se chamam Kafka.

SNS e SQS estão na camada permanentemente gratuita da AWS, não nos seis meses de crédito
de conta nova. São cerca de um milhão de publicações e um milhão de requisições de fila por
mês, sem prazo. Para um sistema com quatro serviços e o volume de uma vitrine municipal, isso
não é apertado.

O levantamento da 0002 continua correto no que apurou: Redpanda com 14 dias, Confluent com
crédito promocional, Upstash com o produto de Kafka descontinuado, e máquina no Render que
hiberna. Nada daquilo mudou. O que mudou é que a pergunta era outra.

## Decisão

Entra um terceiro adaptador de `TransporteDeEventos`, e nada mais muda.

| Adaptador | Onde roda | O que ele é |
|---|---|---|
| `TransporteKafka` | docker-compose, Kubernetes, teste de integração | o padrão |
| `TransporteSns` | implantação gerenciada | publica no SNS, entrega via SQS |
| `TransporteNoProcesso` | demonstração num JAR só | entrega ao despachante local |

O desenho de entrega é o de distribuição: um tópico SNS por assunto, uma fila SQS por serviço,
e a fila assina os tópicos que aquele serviço consome. Isso reproduz o grupo de consumo do
Kafka de forma bem direta: cada serviço recebe a própria cópia da mensagem, e subir uma
segunda instância do mesmo serviço divide a mesma fila em vez de processar tudo duas vezes.

Três detalhes que não são óbvios e custaram tempo:

**Entrega bruta ligada na assinatura.** Sem isso o SNS embrulha a mensagem num envelope
próprio, e o consumidor do SQS precisaria desembrulhar antes de desserializar. O caminho do
SQS deixaria de ser igual ao do Kafka justamente onde os dois têm que ser iguais.

**O tópico de origem viaja num atributo de mensagem.** Uma fila só recebe de vários tópicos, e
sem o atributo o consumidor não sabe qual assunto chegou. Com o envelope desligado, essa
informação não está no corpo.

**A fila precisa de política liberando o SNS.** Fila nasce com permissão só para o dono, e o
SNS é outro serviço. Sem a política, a assinatura é criada, o console mostra tudo verde, e a
mensagem não chega. Não há erro em lugar nenhum.

Nome de recurso na AWS não aceita ponto, e os tópicos daqui usam ponto. A tradução mora numa
classe só, porque o nome tem que sair idêntico na criação do tópico, na criação da fila e na
assinatura; se um dos três divergir, nada falha e a mensagem some.

## O que se perde

**Ordenação.** O Kafka garante ordem dentro da partição, e a chave do evento é o que põe todos
os eventos de um empreendedor na mesma partição. Fila comum do SQS não garante ordem nenhuma.

Isso é aceitável aqui, e vale explicar por quê em vez de só afirmar. Os consumidores já são
idempotentes pelo inbox, então reentrega não duplica efeito. O que sobra é o caso de duas
mensagens do mesmo empreendedor chegarem trocadas, e nos fluxos deste sistema o estado final
não depende dessa ordem: aprovação e suspensão são comandos sobre o cadastro, não incrementos
sobre o anterior.

Existe fila FIFO no SQS, que garante ordem por grupo de mensagem e seria o equivalente exato
da partição. Não foi usada porque tem cota gratuita separada e menor, e porque o problema que
ela resolve não aparece aqui. Se aparecer, a troca é de configuração, não de desenho.

**Vazão e retenção.** Kafka guarda o registro e permite reler do começo; SQS apaga a mensagem
depois de consumida e guarda por no máximo catorze dias. A projeção da busca que nasce vazia e
se reconstrói lendo o tópico desde o início, citada na 0002, só funciona com Kafka. No SNS a
reconstrução precisaria de outro caminho.

## Consequências

O código de domínio não foi tocado. Entraram quatro classes na plataforma, e nenhuma linha
mudou no outbox, no inbox, no despachante ou em qualquer consumidor. O evento continua saindo
pelo outbox na mesma transação do estado, o consumo continua idempotente, e falha continua
voltando para nova tentativa.

O tratamento de mensagem envenenada continua existindo, por outro mecanismo. No Kafka é o
tratador de erro com fila morta; no SQS é a política de redirecionamento da própria fila, com
limite de recebimentos. O comportamento observável é o mesmo: depois de algumas tentativas a
mensagem sai do caminho em vez de travar o consumo.

A troca de transporte é uma variável de ambiente: `TRANSPORTE_DE_EVENTOS=sns`. Credencial vem
do papel da máquina quando roda na AWS, e só precisa de chave e segredo quando roda fora.

## O que ficou aprendido

Abstração só prova que valeu a pena na terceira implementação. Com duas ainda pode ser
coincidência, ou pode ser que as duas tenham sido desenhadas juntas. Esta entrou depois,
escrita contra uma interface que já estava fechada, sem mudar nada do outro lado.

E a premissa da 0002 não estava desatualizada, estava mal pesquisada desde o começo. Vale
relembrar sempre que uma decisão de arquitetura começar com "não existe": em geral existe,
com outro nome.
