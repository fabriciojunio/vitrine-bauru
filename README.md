# Vitrine Bauru

Vitrine digital dos pequenos empreendedores atendidos pela SEDECON, a
Secretaria de Desenvolvimento Econômico de Bauru. O consumidor procura por
produto, bairro ou categoria e fala direto no WhatsApp de quem produz. Não há
carrinho, não há pagamento e não há taxa: a plataforma leva um até o outro e
sai do caminho.

Nasceu como projeto de extensão universitária do Unisagrado, na disciplina de
Sociologia da Responsabilidade Social, e foi construído para funcionar de
verdade, e não para ser demonstrado uma vez e desligado.

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

**4. Não existe Kafka gerenciado de graça em 2026.** O projeto precisa ficar no
ar sem custo. A resposta foi transformar o transporte numa
[interface com dois adaptadores](docs/adr/0002-transporte-de-eventos.md): Kafka
onde há broker, entrega dentro do processo onde não há. O outbox, o inbox, a
transação e os consumidores são exatamente os mesmos nos dois casos.

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
mvn verify                    # 897 testes de back-end
cd web && npm test            # 190 testes de front-end
cd web && npm run e2e         # 36 testes com navegador de verdade
```

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

## Equipe

Projeto de extensão do curso de Ciência da Computação do Unisagrado, em
parceria com a SEDECON.

| Nome | RA |
|---|---|
| Camila Pereira Raimundo | 24111685 |
| Fabrício Júnio Almeida Dias | 24110063 |
| João Pedro Ferreira | 24110920 |
| Kauã Limão Nunes | 24110224 |
| Luan Padilha Miranda | 24110636 |

Orientação: Profa. Dra. Jessica de Cássia Rossi. Contato na SEDECON: Jurandir
Sérgio Posca, Casa do Empreendedor, Av. Duque de Caxias, 16-55, Vila Cardia.
