# Vitrine Bauru

*[Read this in English](README.en.md)*

Vitrine digital dos pequenos empreendedores atendidos pela SEDECON, a
Secretaria de Desenvolvimento Econômico de Bauru. O consumidor procura por
produto, bairro ou categoria e fala direto no WhatsApp de quem produz. Não há
carrinho, não há pagamento e não há taxa: a plataforma leva um até o outro e
sai do caminho.

Nasceu como projeto de extensão universitária do Unisagrado, na disciplina de
Sociologia da Responsabilidade Social, e foi construído para funcionar de
verdade, e não para ser demonstrado uma vez e desligado.

**No ar:** [vitrine-bauru.vercel.app](https://vitrine-bauru.vercel.app) · a API
responde em [vitrine-bauru-api.onrender.com](https://vitrine-bauru-api.onrender.com/swagger-ui.html)

A instância é da camada gratuita e dorme depois de quinze minutos sem acesso: a
primeira abertura pode levar um minuto para responder.

## O problema

A SEDECON já atende esse público pela Casa do Empreendedor, pelo Banco do Povo
e pelo Emprega Bauru. O que falta a quem faz bolo de pote na Vila Cardia ou
conserta máquina de lavar no Mary Dota não é apoio, é alcance: o cliente dele é
quem já o conhece. Uma vitrine comum, com o selo de quem a cidade confia,
resolve exatamente esse pedaço.

Três decisões de produto vieram daí, e explicam quase tudo no código:

**O consumidor não tem conta.** Exigir cadastro para ver o que a padaria do
bairro vende afastaria justamente quem a plataforma quer alcançar. Toda a
vitrine é pública.

**A venda acontece fora da plataforma.** O botão leva ao WhatsApp com a
mensagem pronta. O que dá para medir com honestidade, então, não é
faturamento: é quantas vezes alguém quis falar com um empreendedor por causa
da vitrine. É esse número que vai para o relatório de impacto.

**Ninguém entra sem passar pela SEDECON.** A plataforma leva o nome da
prefeitura, e um golpe aplicado por cadastro falso queimaria a confiança que a
secretaria levou anos construindo. Todo cadastro passa por moderação.

## Como está montado

Quatro serviços em Spring Boot conversando por evento, e um front-end em React.

```
                    consumidor (sem conta)          empreendedor        SEDECON
                             │                            │                │
                             ▼                            ▼                ▼
                     ┌───────────────────────────────────────────────────────┐
                     │                    borda                              │
                     └───────┬───────────────┬───────────────┬───────────────┘
                             ▼               ▼               ▼
                    ┌────────────┐   ┌─────────────┐   ┌──────────────┐
                    │   busca    │   │  catálogo   │   │   cadastro   │
                    │ (vitrine)  │   │ (produtos)  │   │ (moderação)  │
                    └─────┬──────┘   └──────┬──────┘   └──────┬───────┘
                          │                 │                 │
       vitrine.contatos   │  vitrine.catalogo│  vitrine.empreendedores
                          └────────┬────────┴────────┬────────┘
                                   ▼                 ▼
                          ┌────────────────┐  ┌──────────────┐
                          │  notificações  │  │ vitrine.     │
                          │    (e-mail)    │  │ privacidade  │
                          └────────────────┘  └──────────────┘
```

Cada serviço tem o próprio banco e nunca lê a tabela do vizinho. Existe um
[teste de arquitetura](servidor-unico/src/test/java/br/com/vitrinebauru/unico/ArquiteturaTest.java)
que quebra o build se alguém tentar, o que importa porque na implantação
gratuita todos rodam no mesmo processo, e ali nada impediria o atalho.

## Tecnologias

Tudo que está aqui está no `pom.xml` ou no `package.json`, e nada foi escolhido
por catálogo: cada linha resolve um problema que aparece no projeto.

### Back-end

| O que | Versão | Para quê |
|---|---|---|
| Java | 21 | records, `sealed interface` e pattern matching nos contratos de evento |
| Spring Boot | 3.5.16 | base dos quatro serviços e do servidor único |
| Spring Web (MVC) | starter | a API REST de cada serviço |
| Spring Data JPA + Hibernate | starter | persistência e o mapeamento das entidades |
| Spring Security | starter | cadeia de filtros, autorização por papel e bcrypt |
| Spring Validation | starter | validação de entrada nos controladores |
| Spring AOP | starter | corte transversal do disjuntor e da auditoria |
| Spring Kafka | starter | o adaptador de transporte quando existe corretor |
| Spring Cloud Gateway MVC | 2025.0.0 | a borda: um endereço só, CORS e limite de ritmo |
| Spring Boot Actuator | starter | `/health` para o Render e para o Kubernetes |
| PostgreSQL (driver JDBC) | do Boot | o banco de todos os serviços |
| Flyway | core + postgresql | migração versionada, uma faixa de versões por serviço |
| Apache Kafka | via Spring Kafka | tópico por assunto, partição por empreendedor |
| AWS SDK v2 (SNS e SQS) | 2.46.7 | o adaptador gerenciado: tópico por assunto, fila por serviço |
| jjwt | 0.12.6 | emissão e conferência do token de acesso |
| Bucket4j | `bucket4j_jdk17-core` | limite de requisições por endereço, em balde de fichas |
| jsoup | 1.21.1 | sanitização do texto que o empreendedor escreve |
| Resilience4j | Spring Boot 3 | disjuntor na consulta de CNPJ, que é serviço de terceiro |
| Micrometer + Prometheus | registry | métrica do outbox, dos contatos e da fila de moderação |
| springdoc-openapi | 2.8.6 | a documentação da API que sai do próprio código |

### Testes do back-end

| O que | Para quê |
|---|---|
| JUnit 5 | base de tudo, com teste parametrizado sobre amostra gerada |
| AssertJ | asserção legível, que vem no starter de teste |
| Mockito | dublê onde a dependência é de terceiro |
| Spring Boot Test + MockMvc | o serviço inteiro no ar, batendo na API de verdade |
| `embedded-postgres` (zonky) | PostgreSQL de verdade iniciado pelo próprio teste |
| `spring-kafka-test` | corretor Kafka embutido, também sem Docker |
| Awaitility | espera o efeito assíncrono chegar, sem `sleep` cravado |
| ArchUnit | treze regras de arquitetura que quebram o build |
| JaCoCo | cobertura, com piso configurado |

Não há Testcontainers, e é de propósito: o banco e o corretor sobem embutidos
dentro do próprio teste, então o build inteiro roda numa máquina sem Docker
instalado. O teste que prova o outbox de ponta a ponta só vale se rodar em todo
build, e não quando alguém lembra de subir a infraestrutura.

### Front-end

| O que | Versão | Para quê |
|---|---|---|
| React | 19 | a interface inteira |
| TypeScript | 5.7 | modo estrito, sem `any` solto |
| Vite | 7 | construção e o repasse de `/api` no desenvolvimento |
| React Router | 7 | as rotas, com o filtro da vitrine morando na URL |
| Tailwind CSS | 4 | os tokens de cor e tipografia ficam no CSS, em `@theme` |
| Fontsource | Besley e Archivo | fonte servida do próprio domínio, sem chamar o Google |

### Testes do front-end

| O que | Para quê |
|---|---|
| Vitest | os testes de unidade e de componente |
| Testing Library | consulta por papel e por rótulo, como o leitor de tela faz |
| jsdom | o navegador de mentira dos testes de componente |
| Playwright | navegador de verdade, no computador e num Pixel 5 |

### Observabilidade

| O que | Para quê |
|---|---|
| Micrometer + Prometheus | métrica por serviço, que responde "está lento" |
| Micrometer Tracing + OpenTelemetry | rastro distribuído, que responde "está lento onde" |
| datasource-proxy | um trecho por consulta ao banco, que é onde o N mais um aparece |
| Jaeger | o painel onde a linha do tempo do pedido aparece, no compose |
| Spring Boot Actuator | `/health` para o Render e para o Kubernetes |

### Infraestrutura

| O que | Para quê |
|---|---|
| Docker | imagem de duas etapas: compila numa, e a que sobe leva só o JRE |
| Docker Compose | os quatro serviços separados, com Redpanda e quatro bancos |
| Kubernetes | os cinco serviços com Service, PDB, Ingress e HPA, conferidos no CI |
| GitHub Actions | três jobs: back-end, front-end e ponta a ponta |
| Neon | o PostgreSQL da implantação gratuita |
| Render | o back-end, em contêiner |
| Vercel | o front-end |

## Os quatro problemas que este projeto é realmente sobre

**1. Escrever no banco e avisar os outros são duas coisas.** Aprovar um
cadastro e publicar "cadastro aprovado" não cabem na mesma transação. Publicar
primeiro perde a mensagem se o processo cair; salvar primeiro deixa a loja
aprovada que nunca apareceu na busca. Resolvido com
[outbox transacional](docs/adr/0003-outbox-e-inbox.md): o evento é gravado na
mesma transação do estado, e um publicador separado o envia. Troca "pode
perder evento" por "pode enviar duas vezes", que tem resposta conhecida.

**2. A mensagem chega duas vezes.** Todo consumidor grava no inbox o par
(evento, consumidor) antes de agir, dentro da mesma transação do trabalho. A
segunda entrega encontra a marca e não faz nada. Sem isso, um rebalanceamento
do broker mandaria dois e-mails de aprovação para a mesma pessoa.

**3. Apagar dado pessoal é uma conversa entre quatro serviços.** O pedido de
exclusão da LGPD não é um `delete`: o produto está no catálogo, a projeção está
na busca e o histórico de e-mail está em notificações, cada um no seu banco.
É uma [saga](docs/adr/0004-saga-de-exclusao-lgpd.md) com prazo, reenvio para
quem não respondeu e alerta quando o prazo legal estoura. Não há compensação,
e isso é assumido: exclusão não se desfaz.

**4. O projeto precisa ficar no ar sem custo, e o transporte virou uma
interface por causa disso.** Não há Kafka gerenciado com camada gratuita
permanente, então o transporte ganhou
[adaptadores intercambiáveis](docs/adr/0002-transporte-de-eventos.md): Kafka
onde há corretor, entrega dentro do processo onde não há. O outbox, o inbox, a
transação e os consumidores são idênticos nos dois casos.

Depois apareceu o terceiro. Eu tinha escrito naquele documento que mensageria
gerenciada gratuita não existia, e estava errado: eu havia procurado por Kafka
gerenciado, não pelo problema. SNS e SQS estão na camada permanentemente
gratuita da AWS, e o
[adaptador de SNS](docs/adr/0007-transporte-sns.md) entrou sem tocar em uma
linha de domínio. O que se perde é a ordenação por chave, e o documento explica
por que aqui isso não custa caro.

**5. O outbox corta o rastro no meio.** O evento é gravado na transação de
quem atendeu a requisição e publicado depois, por outra thread, quando aquela
requisição já acabou. O contexto de rastro vive na thread, então no commit ele
morre, e o painel mostra dois rastros desligados em vez de um pedido inteiro.
A solução é a mesma ideia do outbox aplicada à observabilidade: o que atravessa
transação precisa ser gravado. O `traceparent` do W3C vai numa coluna, e depois
em cabeçalho do Kafka ou atributo do SNS.
[O documento](docs/adr/0008-rastro-distribuido.md) explica por que trecho
filho e não continuação, e por que o rastreamento vem desligado por padrão.

**6. Migração de esquema pode derrubar a versão anterior do código.** A atualização é gradual e
duas réplicas rodam ao mesmo tempo, então uma migração que apaga coluna que a versão antiga ainda
lê quebra quem estiver usando o sistema naquele instante. A resposta é expandir, migrar e só
depois contrair, em três implantações. E o que segura isso não é o documento, é uma regra no
build: `MigracaoSemQuebraTest` reprova comando destrutivo que não venha com a linha
`-- contrair:` e o motivo escrito.
[O documento](docs/adr/0009-migracao-sem-parada.md) explica os três passos e o que fica de fora.

## Rodando

Precisa de Java 21, Node 22 e Docker.

```bash
git clone https://github.com/fabriciojunio/vitrine-bauru.git
cd vitrine-bauru

# Sobe broker, quatro bancos, quatro serviços e a borda.
docker compose up -d

# Front-end
cd web && npm install && npm run dev
```

A vitrine abre em `http://localhost:5173` e a API na porta 8080. O modo
demonstração vem ligado no compose: a tela de entrar tem dois botões, um para
cada lado do sistema.

Sem Docker, dá para subir tudo num processo só:

```bash
mvn -pl servidor-unico -am package -DskipTests
BANCO_URL=jdbc:postgresql://localhost:5432/vitrine DEMO_ATIVO=true \
  java -jar servidor-unico/target/servidor-unico-1.0.0.jar
```

## Testes

```bash
mvn verify                    # 813 testes de back-end
cd web && npm test            # 193 testes de front-end
cd web && npm run e2e         # 36 testes com navegador de verdade
node web/scripts/auditoria-de-celular.mjs   # varredura de tela estreita
python k8s/conferir-manifestos.py           # coerência dos manifestos
```

A varredura de celular abre as sete telas em 320px e 393px e reporta o que
costuma quebrar no dedo: rolagem horizontal, alvo de toque menor que o mínimo,
texto abaixo de 12px e erro de script. Precisa do ambiente no ar em
`localhost:4173`.

Os testes de integração sobem PostgreSQL e Kafka embutidos, iniciados pelo
próprio teste. Nenhum deles precisa de Docker, e é de propósito: o teste que
prova o outbox funcionando de ponta a ponta só vale se rodar em todo build, e
não quando alguém lembra de subir a infraestrutura.

O que os testes cobrem, além do caminho feliz:

- a matriz inteira de transições da moderação, inclusive as que precisam falhar;
- dez pessoas se cadastrando ao mesmo tempo com o mesmo e-mail, com threads de
  verdade contra um banco de verdade;
- CPF e CNPJ sobre amostra gerada, incluindo o
  [CNPJ alfanumérico](contratos/src/main/java/br/com/vitrinebauru/contratos/tipos/Documento.java)
  que a Receita passou a emitir em julho de 2026;
- upload de arquivo que se diz JPEG e é HTML com script dentro;
- injeção de SQL no campo de busca, CORS de origem desconhecida e força bruta
  no login;
- a saga de exclusão inteira, com um serviço confirmando duas vezes.

## Documentação

| Documento | O que tem lá |
|---|---|
| [Arquitetura](docs/ARQUITETURA.md) | Os quatro serviços, os tópicos, o que cada um guarda |
| [Decisões](docs/adr/) | Por que cada escolha foi feita, e o que se perdeu com ela |
| [Implantação](docs/IMPLANTACAO.md) | Como colocar no ar de graça, passo a passo |
| [Demonstração](docs/DEMONSTRACAO.md) | Como funciona o modo de demonstração e como reiniciá-lo |
| [Identidade visual](docs/IDENTIDADE_VISUAL.md) | De onde vem o visual e como mexer nele sem quebrar o conjunto |
| [Segurança](SECURITY.md) | O que foi feito, o que não foi, e como avisar de uma falha |
| [Contribuindo](CONTRIBUTING.md) | Padrão de branch, commit e revisão do grupo |

## O que este projeto não faz

Vale escrever, porque é o tipo de coisa que aparece na banca:

- **Não processa pagamento.** Estava fora do escopo desde o começo, e o modelo
  de dados de produto foi desenhado para permitir acrescentar depois sem
  reescrever nada.
- **Não tem mapa.** Filtro por bairro em texto resolve o que o consumidor
  precisa hoje, e mapa exige chave de API paga ou cartão cadastrado.
- **Não confirma o telefone por SMS.** Tem custo por mensagem, e a aprovação
  manual da SEDECON já cobre boa parte do risco.
- **Não usa `pg_trgm` na busca.** O texto é normalizado na gravação e a
  consulta é um `like` comum, que roda em qualquer PostgreSQL gratuito. Com
  algumas centenas de lojas, a diferença não é perceptível; o dia em que for, o
  índice entra sem mexer em mais nada.
