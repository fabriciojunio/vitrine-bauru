# Modo demonstração

O modo demonstração existe para quem vai avaliar o sistema (a professora, a
SEDECON, quem abrir o portfólio) não precisar inventar um cadastro para ver
como ele é por dentro.

## Como funciona

Com `DEMO_ATIVO=true`, três coisas mudam:

1. **A tela de entrar ganha dois botões**, um por papel. Quem responde se a
   demonstração está ligada é o servidor, então a mesma imagem da aplicação
   sobe em demonstração e em produção sem nenhuma diferença de código: em
   produção o endereço simplesmente não existe e os botões somem.
2. **Uma faixa amarela aparece no topo de todas as telas**, avisando que as
   lojas e os telefones são fictícios. Sem isso, alguém tentaria comprar um
   bolo que não existe.
3. **A base é semeada na primeira subida**, com doze lojas e trinta e oito
   produtos.

## O que está lá dentro

Doze negócios fictícios em bairros reais de Bauru, escolhidos para cobrir os
casos que o sistema precisa saber tratar:

| Loja | Bairro | Por que ela está na lista |
|---|---|---|
| Doces da Lourdes | Vila Cardia | Tem produto com preço e produto sob consulta |
| Marcenaria Irmãos Pereira | Jardim Europa | Quase tudo é sob consulta, como marcenaria é |
| Ateliê Fio de Prosa | Vila Falcão | Tem produto marcado como esgotado |
| Studio Nara Cabelo e Estética | Jardim Estoril | Serviço com hora marcada, e não produto |
| Conserta Tudo Eletro | Núcleo Habitacional Mary Dota | Nome de bairro comprido, que quebra layout mal feito |
| Sabor da Roça Marmitas | Vila Independência | Entrega, e preço por quantidade |
| Pet Amigo Banho e Tosa | Jardim Brasil | Tem serviço com preço zero, que é diferente de sem preço |
| Reforço Escolar Dona Ivone | Vila Universitária | Educação, com pacote mensal |
| Bicicletaria do Zé | Vila Seabra | Nome com acento no fim |
| Costura e Cia Reformas de Roupa | Centro | Loja do centro, sem foto |
| Festa Boa Locação | Parque Vista Alegre | **Fica pendente**, para a fila de moderação ter item |
| Auto Elétrica Central | Distrito Industrial | **Fica pendente**, idem |

As duas últimas ficam esperando análise de propósito. Sem elas, quem entra como
SEDECON encontra uma fila vazia e não consegue ver a parte mais importante do
sistema funcionando.

Os documentos são números gerados só para fechar o dígito verificador. Os
telefones estão na faixa 9971 do DDD 14 e não pertencem a ninguém. Nenhum
empreendedor de verdade atendido pela SEDECON aparece na lista.

## As contas

| Papel | E-mail | O que vê |
|---|---|---|
| Empreendedora | `lourdes@demo.vitrinebauru.com.br` | A loja Doces da Lourdes: catálogo, situação do cadastro, contatos recebidos |
| SEDECON | `sedecon@demo.vitrinebauru.com.br` | Fila de moderação, aprovação, recusa e indicadores |

A senha das duas é `demonstracao2026`, configurável por `DEMO_SENHA`. Os botões
da tela de entrar dispensam digitar qualquer coisa.

## Como a semeadura acontece

Pelos mesmos caminhos que o sistema usa de verdade: cria a conta, cria o
cadastro, chama `aprovar` do domínio e grava os eventos no outbox. Não há
atalho por SQL.

A consequência é que a busca pública se enche sozinha por evento, exatamente
como aconteceria com um cadastro real, e o e-mail de boas-vindas e o de
aprovação são gerados de verdade (e escritos no log, já que o envio real fica
desligado).

A única exceção é o catálogo, que grava os produtos direto no repositório. O
motivo está comentado no código: o caso de uso exige que o empreendedor já seja
conhecido do catálogo, e ele só passa a ser quando o evento de cadastro chega,
o que ainda não aconteceu no instante em que a aplicação está subindo.

## Reiniciando

A demonstração é pública e qualquer pessoa aprova cadastro e publica produto
nela. O fluxo `reiniciar-demonstracao.yml` apaga os esquemas de madrugada e
deixa o Flyway reconstruir na subida seguinte, o que semeia tudo de novo.

Para reiniciar à mão, apague os esquemas e reinicie o serviço:

```sql
drop schema if exists cadastro cascade;
drop schema if exists catalogo cascade;
drop schema if exists busca cascade;
drop schema if exists notificacoes cascade;
drop table if exists outbox;
drop table if exists inbox;
drop table if exists flyway_schema_history;
```

## Nunca em produção

`DEMO_ATIVO` liga um endereço que devolve sessão válida sem senha nenhuma. O
padrão é desligado, e ligar precisa ser um ato deliberado do ambiente. No dia
em que a plataforma tiver dado de gente de verdade, essa variável fica fora.
