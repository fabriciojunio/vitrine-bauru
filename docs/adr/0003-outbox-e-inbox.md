# 3. Outbox na saída, inbox na entrada

Data: 2026-08-30
Situação: aceita

## Contexto

Aprovar um cadastro faz duas coisas: muda uma linha no banco do cadastro e
avisa o resto do sistema. São dois sistemas diferentes e não existe transação
cobrindo os dois.

Publicar primeiro e salvar depois: se o processo cair no meio, o resto do
sistema acredita numa aprovação que não aconteceu, e a loja aparece na busca
enquanto o cadastro segue pendente.

Salvar primeiro e publicar depois: se o processo cair no meio, o cadastro está
aprovado e ninguém sabe. A loja nunca aparece, o e-mail nunca sai, e o
empreendedor liga para a SEDECON perguntando por que continua fora do ar.

O segundo é mais silencioso, e por isso pior: não gera erro em lugar nenhum.

## Decisão

**Na saída, outbox.** O evento é gravado numa tabela do mesmo banco, dentro da
mesma transação que mudou o estado. Um publicador agendado lê a tabela e
entrega ao transporte. A ordem é publicar primeiro e marcar depois: uma queda
entre as duas coisas faz a mensagem sair de novo, o que é aceitável, em vez de
marcá-la como enviada sem ter saído, o que não é.

A chave primária da tabela é o id do próprio evento, e não um id gerado ali.
Isso transforma o banco em guarda contra duplicata: se a mesma regra rodar duas
vezes por causa de uma repetição, a segunda gravação esbarra na chave.

**Na entrada, inbox.** Todo consumidor grava o par (evento, consumidor) antes
de agir, dentro da mesma transação do trabalho. A segunda entrega encontra a
marca e não faz nada. A chave é o par, e não só o evento, porque dois
consumidores diferentes reagindo ao mesmo evento são dois trabalhos legítimos.

## Consequências

A garantia passa a ser "ao menos uma vez", nunca zero. Isso empurra uma
exigência para todo consumidor: ser idempotente. Não é opcional nem
"recomendado", é o que faz o desenho funcionar.

Duas tabelas extras por banco, e duas rotinas agendadas que rodam sempre. O
custo é pequeno e visível.

O outbox crescendo é o primeiro sinal de que o transporte quebrou, e aparece
antes de qualquer reclamação de empreendedor. Por isso é métrica, e não só log:
`vitrine.outbox.pendentes` e `vitrine.outbox.travadas`.

A espera entre tentativas dobra, de dois segundos até dez minutos. Sem isso, um
broker fora do ar viraria uma consulta ao banco a cada meio segundo por
mensagem parada, e o outbox derrubaria o banco tentando consertar a falta do
broker.

## Detalhe que custou um bug

O publicador usa `for update skip locked` ao ler as pendentes. Sem isso, duas
instâncias do mesmo serviço leriam as mesmas linhas e publicariam a mesma
mensagem duas vezes. Com o `skip locked`, quem chega depois pula o que está
travado e a fila continua andando, em vez de esperar.

Um segundo caso, esse encontrado por teste de integração e não por leitura:
quando o caso de uso termina lançando exceção, a transação inteira volta atrás,
e com ela qualquer registro feito no caminho. Foi o que aconteceu com o
contador de senha errada, que subia e era desfeito pela mesma exceção que ele
existia para registrar. A correção foi dar transação própria ao registro, e
está em `RegistroDeSeguranca`.
