# 9. Migração de esquema sem derrubar a versão anterior

Data: 2026-09-02
Situação: aceita

## Contexto

Os manifestos do Kubernetes sobem duas réplicas por serviço e a atualização é gradual. Isso
significa que, durante alguns segundos a cada implantação, a versão nova e a antiga do código
estão rodando ao mesmo tempo, contra o mesmo banco.

O Flyway roda a migração na subida da primeira instância nova. Se essa migração apaga uma coluna
que a versão antiga ainda lê, a janela de convivência vira erro para quem estiver usando o
sistema naquele instante. O mesmo vale para renomear coluna e para trocar o tipo dela.

É um problema que não aparece em desenvolvimento, porque lá só existe uma versão do código. Só
aparece na primeira implantação depois de alguém escrever a migração.

## Decisão

Mudança de esquema incompatível acontece em três implantações separadas, e não em uma.

**Expandir.** A coluna nova entra ao lado da antiga, aceitando nulo. O código passa a escrever
nas duas e a ler ainda da antiga. Nada quebra, porque nada foi tirado.

**Migrar.** Os dados antigos são copiados para a coluna nova, e o código passa a ler da nova. A
antiga continua lá, ainda sendo escrita, o que é o que torna possível voltar atrás sem perder o
que entrou no meio do caminho.

**Contrair.** Só depois de a versão anterior não existir mais em ambiente nenhum, a coluna antiga
sai. É aqui que o comando destrutivo entra, e é o único lugar onde ele é seguro.

A coluna `trace_pai` do documento 0008 é um exemplo do primeiro passo, e é por isso que ela
aceita nulo mesmo sendo escrita sempre.

## O que garante isso

Processo escrito em documento é esquecido. O que segura é uma regra no build.

`MigracaoSemQuebraTest` lê todos os arquivos de migração do repositório e reprova comando
destrutivo: apagar coluna, renomear coluna, trocar o tipo e apertar para não nulo. Os quatro
quebram a versão anterior enquanto ela ainda roda.

O teste não impede o terceiro passo, que é legítimo. Ele exige que quem o escreve declare no
próprio arquivo:

```sql
-- contrair: a versão 1.4 saiu de todos os ambientes em 02/09/2026
alter table outbox drop column coluna_antiga;
```

A marca precisa de um motivo na mesma linha. Sem essa exigência ela viraria um comentário colado
para o build passar, o que é pior que não ter regra: dá a impressão de proteção sem proteger.

Detalhe de implementação que quase passou: o padrão que reconhece a marca usava `\s`, que
atravessa quebra de linha. Com isso, uma marca vazia engolia o próprio comando abaixo dela como
se fosse a justificativa, e liberava tudo. O teste que pega isso é o `marcaSemMotivoNaoLibera`.

## A armadilha das faixas por serviço

Esta decisão foi escrita e, no mesmo dia, a primeira migração feita sob ela derrubou o ambiente.
Vale registrar, porque o erro é do tipo que não aparece em desenvolvimento nenhum.

As migrações são organizadas em faixas: 1.x é a plataforma, 2.x o cadastro, 3.x o catálogo, 4.x a
busca, 5.x as notificações. Cada serviço carrega só a própria faixa mais a da plataforma, e por
isso as faixas nunca se cruzam do ponto de vista de um serviço.

O servidor único carrega todas. E o Flyway aplica e valida em ordem numérica **global**,
recusando por padrão uma migração cuja versão seja menor que a última já aplicada. A `V1_2` da
plataforma entrou num banco que já tinha a 5.1 das notificações, e a subida passou a falhar com
`Detected resolved migration not applied to database: 1.2`.

Localmente nada disso aparece: o banco de teste nasce vazio e recebe as migrações na ordem em que
o Flyway as ordena. O erro só existe contra um banco que já tem histórico, que é exatamente o de
produção.

São dois caminhos. Abandonar a faixa por serviço, e aí toda migração nova precisa saber o número
global mais alto, o que amarra os serviços uns aos outros justamente no ponto em que o projeto
tenta separá-los. Ou aceitar ordem fora de sequência, com `spring.flyway.out-of-order`. Foi o
segundo, porque preserva a independência que as faixas existem para dar.

O custo assumido: com ordem livre, uma migração antiga esquecida num galho e trazida depois é
aplicada em silêncio, em vez de reprovar a subida. Em time grande isso é um risco de verdade.
Aqui, com faixas explícitas e revisão do que entra, é menor que o de amarrar as numerações.

## O que isto não cobre

Não cobre migração de dados demorada. Copiar milhões de linhas dentro do Flyway trava a subida da
aplicação e o Kubernetes mata o pod pela sonda de vida. Nesse caso o passo dois precisa ser um
trabalho em lotes, fora da migração, e não há nada aqui que force isso.

Não cobre a decisão de quando a versão anterior realmente saiu de circulação. Isso é operação, e
depende de olhar o que está rodando, não o repositório.

E não cobre índice criado sem `concurrently`, que trava escrita na tabela enquanto é construído.
Fica como o próximo item, porque exige distinguir tabela grande de tabela pequena, e o teste hoje
não tem como saber isso lendo o arquivo.
