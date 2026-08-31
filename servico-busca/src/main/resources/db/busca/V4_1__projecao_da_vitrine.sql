-- Projecao publica: a unica parte do sistema aberta sem login.
--
-- Nao ha chave estrangeira entre produto e loja de proposito. Os dois vem de
-- topicos diferentes e nada garante qual chega primeiro; uma restricao aqui
-- faria o consumidor de eventos falhar e reprocessar em laco toda vez que o
-- produto chegasse na frente da loja.

create schema if not exists busca;

create table busca.loja (
    id                uuid         primary key,
    nome_do_negocio   varchar(120) not null,
    apelido_na_url    varchar(60)  not null,
    descricao         varchar(600),
    categoria         varchar(60)  not null,
    bairro            varchar(60)  not null,
    telefone_whatsapp varchar(11)  not null,
    foto_de_capa_url  varchar(400),
    visivel           boolean      not null default false,
    busca             varchar(900) not null,
    atualizada_em     timestamptz  not null
);

create unique index idx_loja_apelido on busca.loja (apelido_na_url);

-- Indice parcial: quase toda consulta filtra por visivel, e loja escondida
-- nao precisa ocupar espaco no indice da vitrine.
create index idx_loja_visivel on busca.loja (bairro, categoria) where visivel;

create table busca.produto (
    id                uuid          primary key,
    empreendedor_id   uuid          not null,
    nome              varchar(120)  not null,
    descricao         varchar(800),
    preco_em_centavos bigint,
    categoria_nome    varchar(60)   not null,
    imagem_url        varchar(400),
    disponivel        boolean       not null default false,
    loja_nome         varchar(120),
    loja_apelido      varchar(60),
    bairro            varchar(60),
    visivel           boolean       not null default false,
    busca             varchar(1100) not null,
    atualizado_em     timestamptz   not null
);

create index idx_produto_da_loja on busca.produto (empreendedor_id);

create index idx_produto_na_vitrine
    on busca.produto (atualizado_em desc)
    where visivel and disponivel;

create index idx_produto_filtros
    on busca.produto (bairro, categoria_nome)
    where visivel and disponivel;

comment on column busca.loja.busca is
    'Nome, descricao, categoria e bairro em minusculas e sem acento, para a busca por palavra';
comment on column busca.produto.visivel is
    'Espelha a visibilidade da loja: produto de loja suspensa sai da vitrine junto';
