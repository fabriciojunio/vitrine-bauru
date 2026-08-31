# 5. Foto de produto guardada no banco

Data: 2026-08-30
Situação: aceita, com data para revisar

## Contexto

O documento de integrações do projeto previa Cloudflare R2 ou Supabase Storage
para as fotos, com envio direto do navegador por URL assinada. É o desenho
certo para um sistema que cresce, e a recomendação continua válida.

O problema é o que ele exige antes de funcionar: conta em um provedor, cartão
de crédito cadastrado em alguns casos, credencial de acesso guardada em algum
lugar e um bucket configurado. Nada disso existe num projeto de disciplina com
cinco alunos, e o pedaço "alguém do grupo cadastra o cartão dele" é justamente
o que trava a entrega.

Guardar arquivo no disco do servidor está fora de discussão: some no primeiro
redeploy, e a camada gratuita reinicia o serviço sozinha.

## Decisão

A foto vai para uma coluna `bytea`, numa tabela própria do serviço de catálogo,
com limite de 5 MB por arquivo e 50 MB por loja.

O tipo é descoberto pelos primeiros bytes do arquivo, e não pela extensão nem
pelo cabeçalho enviado: os dois são escritos por quem envia, e um arquivo
chamado `foto.jpg`, anunciado como `image/jpeg`, pode ser HTML com script
dentro. Servido de volta no mesmo domínio da API, ele executaria.

O acesso fica atrás de uma interface, para trocar por bucket sem tocar em regra
nenhuma.

## Conta que sustenta a decisão

Algumas centenas de lojas, com dez a vinte produtos cada, com fotos de celular
comprimidas em torno de 300 KB. Dá algo entre 300 MB e 900 MB no pior caso, e
o plano gratuito do Neon oferece 500 MB, o que já cobre o volume real esperado
de um projeto que começa. O limite por loja existe justamente para uma sozinha
não consumir o espaço de todas.

## Consequências

**O que se ganha.** Funciona hoje, sem conta em lugar nenhum, sem credencial e
sem cartão. O backup do banco leva as fotos junto. Apagar o produto apaga a
foto, sem objeto órfão em bucket, que é o vazamento silencioso mais comum
quando se guarda arquivo fora do banco.

**O que se perde.** O banco cresce rápido e o backup fica pesado. Não há CDN:
cada foto sai do servidor, o que consome a banda da camada gratuita. O cache de
uma semana no cabeçalho ajuda, mas não substitui.

**Detalhe que custou tempo.** No PostgreSQL, `@Lob` em `byte[]` vira `oid`, que
guarda o arquivo fora da tabela, num objeto grande com ciclo de vida próprio:
apagar a linha não apaga o arquivo, e o banco vai enchendo de órfão até alguém
rodar limpeza. O mapeamento correto aqui é `bytea`, sem `@Lob`.

## Quando revisar

Quando o banco passar de 300 MB, ou quando a banda começar a pesar. O adaptador
para bucket compatível com S3 é uma classe, e o resto do sistema não fica
sabendo.
