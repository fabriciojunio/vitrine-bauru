# 1. Quatro serviços, separados por assunto

Data: 2026-08-30
Situação: aceita

## Contexto

O sistema tem quatro coisas bem diferentes acontecendo: gente se cadastrando e
sendo moderada, gente cuidando do próprio catálogo, gente procurando produto
sem ter conta, e e-mail sendo disparado quando algo muda.

Cada uma dessas quatro tem um perfil de uso próprio. A busca pública é a única
aberta na internet e a única que sofre pico quando a SEDECON divulga a
plataforma num grupo de bairro. O cadastro guarda CPF e senha. O catálogo
recebe upload de foto, que é a operação mais cara do sistema. As notificações
dependem de um serviço de terceiro que cai.

## Decisão

Quatro serviços, um por assunto, cada um com o próprio banco, conversando por
evento em quatro tópicos.

A separação é por assunto, e não por camada: não existe "serviço de banco de
dados" nem "serviço de regras". Cada um é um pedaço do problema inteiro, com
API, regra e persistência próprias.

## Consequências

**O que se ganha.** A vitrine continua no ar quando o cadastro cai, que é o
caso que mais importa: o consumidor não pode perder a busca porque a moderação
está em manutenção. A busca escala sozinha sem levar junto três serviços que
não precisam. E o dado sensível fica concentrado num banco só: quem lê a
projeção pública não tem CPF ao alcance, porque ele não está lá.

**O que se perde.** Consistência imediata. Entre aprovar um cadastro e a loja
aparecer na busca passam alguns segundos, e a interface precisa dizer isso em
vez de fingir que é instantâneo. Investigar um problema passa a exigir cruzar
log de quatro processos, e é por isso que existe a correlação carimbada em toda
requisição e em todo evento.

**O que fica mais caro.** Quatro processos, quatro bancos e um broker não cabem
em camada gratuita. A resposta a isso está na
[decisão 2](0002-transporte-de-eventos.md).

## Alternativas consideradas

**Um monolito.** Seria mais simples e mais barato, e para o volume esperado
seria suficiente. Foi recusado porque a separação é o que o grupo está
aprendendo a fazer, e porque o desenho por assunto sobrevive ao crescimento sem
reescrita. A concessão está registrada: na implantação gratuita, os quatro
rodam juntos, e o que garante que continuem separados é teste de arquitetura.

**Separar por camada.** Um serviço de API, um de regra e um de dados. É a
divisão que parece organizada no diagrama e que na prática faz toda alteração
tocar os três, porque uma mudança de negócio nunca cabe numa camada só.
