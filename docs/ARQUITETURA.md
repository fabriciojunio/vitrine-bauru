# Arquitetura

Este documento explica como o sistema está montado por dentro. As decisões e o
porquê de cada uma estão nos [registros de decisão](adr/); aqui está o mapa.

## Os módulos

| Módulo | O que é dele | Porta |
|---|---|---|
| `contratos` | Eventos, tópicos e tipos de valor compartilhados. Não depende de nada | — |
| `plataforma` | Outbox, inbox, despachante, segurança, tratamento de erro, limite de requisição | — |
| `servico-cadastro` | Contas, perfil do empreendedor, moderação da SEDECON, indicadores, LGPD | 8081 |
| `servico-catalogo` | Produtos, categorias, fotos | 8082 |
| `servico-busca` | Projeção pública: a vitrine e o registro de contato | 8083 |
| `servico-notificacoes` | E-mail transacional | 8084 |
| `borda` | Roteamento por prefixo, quando os serviços rodam separados | 8080 |
| `servidor-unico` | Empacota os quatro num processo, para a implantação gratuita | 8080 |
| `web` | Vitrine pública e os dois painéis, em React | 5173 |

`contratos` e `plataforma` são bibliotecas: não sobem sozinhas. Os quatro
serviços dependem das duas, e de mais nada um do outro.

## Os tópicos

| Tópico | Quem publica | Quem consome |
|---|---|---|
| `vitrine.empreendedores` | cadastro | catálogo, busca, notificações |
| `vitrine.catalogo` | catálogo | busca, cadastro (contagem de produtos) |
| `vitrine.contatos` | busca | cadastro (indicadores) |
| `vitrine.privacidade` | todos | cadastro (coordenador da saga) |

Um tópico por assunto, e não por serviço: quem publica não precisa saber quem
escuta. A chave de particionamento é sempre o identificador do empreendedor, e
é isso que garante a ordem entre "aprovado" e "suspenso" do mesmo cadastro.
Sem essa chave, dois eventos poderiam ser processados fora de ordem e a loja de
alguém suspenso voltaria ao ar.

Cada tópico tem uma fila morta com sufixo `.dlq`, para onde vai a mensagem que
falhou três vezes seguidas. Mensagem envenenada trava partição, e fila travada
é o jeito silencioso de um sistema de eventos parar.

## O caminho de um cadastro

```
empreendedor          cadastro              catálogo        busca        notificações
     │                    │                     │             │               │
     ├── POST /empreendedores                   │             │               │
     │                    ├─ grava conta + perfil (1 transação)               │
     │                    ├─ grava EmpreendedorCadastrado no outbox           │
     │◄── 201 ────────────┤                     │             │               │
     │                    │                     │             │               │
     │            (publicador do outbox)        │             │               │
     │                    ├──── vitrine.empreendedores ──────►│               │
     │                    │                     ├─ aprende a loja             │
     │                    │                     │             ├─ grava invisível
     │                    │                     │             │               ├─ e-mail de boas-vindas
     │                    │                     │             │               │
SEDECON                   │                     │             │               │
     ├── POST /moderacao/{id}/aprovar           │             │               │
     │                    ├─ muda o estado + grava CadastroAprovado (1 transação)
     │◄── 204 ────────────┤                     │             │               │
     │                    ├──── vitrine.empreendedores ──────►│               │
     │                    │                     │             ├─ torna visível
     │                    │                     │             │               ├─ e-mail "sua loja está no ar"
```

O tempo entre o clique da SEDECON e a loja aparecer na busca é o intervalo do
publicador do outbox, que é meio segundo por padrão. A interface diz isso ao
empreendedor em vez de fingir que é instantâneo.

## O que cada serviço guarda do outro

Nenhum serviço consulta o outro por HTTP. O que eles precisam saber, aprendem
por evento e guardam:

- **catálogo** guarda `empreendedor_conhecido`: id, nome do negócio e se pode
  publicar. Não guarda documento, e-mail nem telefone, porque não precisa.
- **busca** guarda a projeção completa da vitrine, inclusive nome da loja e
  bairro dentro de cada produto. É duplicação de propósito: sem ela, filtrar
  produto por bairro exigiria juntar duas tabelas alimentadas por serviços
  diferentes a cada busca.
- **cadastro** guarda `produto_do_empreendedor` (só id, dono e data) e
  `contato_registrado`, que alimentam o painel de indicadores da SEDECON.

Projeção pode ser reconstruída a partir dos eventos, então a duplicação não
cria uma segunda fonte da verdade.

## Segurança

- Senha com bcrypt de custo 12. O custo fica gravado no hash, então subi-lo no
  futuro não invalida as senhas existentes.
- Token de acesso JWT de 15 minutos, com o papel dentro. É o que permite ao
  catálogo autorizar sem chamar o cadastro.
- Token de renovação de 7 dias, guardado como resumo SHA-256, com rotação a
  cada uso e detecção de reuso: token queimado que reaparece derruba todas as
  sessões daquele usuário.
- CORS restrito ao domínio do front-end, nunca `*`.
- CSP, HSTS, `X-Frame-Options: DENY` e `Referrer-Policy: no-referrer` em toda
  resposta.
- Limite de requisição por endereço, com regras por caminho: cinco tentativas
  de login por minuto, dez cadastros por hora, cento e vinte buscas por minuto.
- Texto livre limpo com jsoup na entrada, além do escape que o React já faz na
  saída. Duas camadas porque a descrição do produto também vai para o corpo do
  e-mail e para o JSON da API, que não passam pelo navegador.

O que deliberadamente não foi feito está em [SECURITY.md](../SECURITY.md).

## Observabilidade

Toda requisição recebe uma correlação, que entra no registro de log, viaja
dentro do evento gravado no outbox e reaparece no log de quem consumiu, do
outro lado. Com quatro serviços e mensagens assíncronas, é o que transforma
"o cadastro do fulano não apareceu na busca" numa investigação de dois minutos
em vez de comparar horário a olho.

As métricas que importam, em `/actuator/prometheus`:

| Métrica | Por que existe |
|---|---|
| `vitrine.outbox.pendentes` | Fila crescendo é o primeiro sinal de transporte quebrado |
| `vitrine.outbox.travadas` | Mensagem que esgotou as tentativas e precisa de gente |
| `vitrine.inbox.repetidos` | Quanto o broker está reentregando |
| `vitrine.evento.consumo` | Tempo por consumidor e por tipo de evento |
| `vitrine.exclusao.fora_do_prazo` | Pedido de LGPD passando do prazo legal |
| `vitrine.email.pendentes` | Provedor de e-mail fora do ar |
| `vitrine.limite.bloqueios` | Quem está batendo no limite, e onde |
