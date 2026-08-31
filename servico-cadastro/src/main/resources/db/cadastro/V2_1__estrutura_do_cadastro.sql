-- Estrutura do servico de cadastro.
--
-- Esquema proprio, e nao tabelas soltas no publico. Em implantacao separada
-- cada servico tem o proprio banco e o esquema e so organizacao; no processo
-- unico da demonstracao, os quatro dividem a mesma conexao, e o esquema e o
-- que impede o catalogo de ler a tabela de usuario do cadastro por descuido.

create schema if not exists cadastro;

create table cadastro.usuario (
    id                uuid         primary key,
    nome              varchar(120) not null,
    email             varchar(160) not null unique,
    senha_hash        varchar(100) not null,
    papel             varchar(20)  not null,
    criado_em         timestamptz  not null,
    ultimo_acesso_em  timestamptz,
    tentativas_falhas integer      not null default 0,
    bloqueado_ate     timestamptz,
    ativo             boolean      not null default true,
    anonimizado_em    timestamptz
);

create table cadastro.empreendedor (
    id                     uuid         primary key,
    usuario_id             uuid         not null unique references cadastro.usuario (id),
    nome_do_negocio        varchar(120) not null,
    apelido_na_url         varchar(60)  not null unique,
    descricao              varchar(600),
    categoria_principal    varchar(60)  not null,
    bairro                 varchar(60)  not null,
    cep                    varchar(8),
    telefone_whatsapp      varchar(11)  not null,
    documento              varchar(14)  not null,
    documento_tipo         varchar(10)  not null,
    foto_de_capa_url       varchar(400),
    status                 varchar(20)  not null,
    criado_em              timestamptz  not null,
    atualizado_em          timestamptz  not null,
    moderado_em            timestamptz,
    moderado_por           uuid,
    motivo_da_moderacao    varchar(400),
    situacao_do_documento  varchar(120),
    documento_conferido_em timestamptz
);

-- Documento unico entre cadastros vivos. O parcial e necessario porque a
-- exclusao de dados troca o documento por zeros, e dois cadastros excluidos
-- teriam o mesmo valor.
create unique index idx_empreendedor_documento
    on cadastro.empreendedor (documento)
    where status <> 'EXCLUIDO';

-- A fila de moderacao e a consulta mais frequente do painel da SEDECON.
create index idx_empreendedor_fila
    on cadastro.empreendedor (criado_em)
    where status = 'PENDENTE';

create index idx_empreendedor_status on cadastro.empreendedor (status);
create index idx_empreendedor_bairro on cadastro.empreendedor (bairro);

create table cadastro.sessao_de_renovacao (
    id              uuid        primary key,
    usuario_id      uuid        not null references cadastro.usuario (id),
    hash_do_token   varchar(64) not null unique,
    criada_em       timestamptz not null,
    expira_em       timestamptz not null,
    usada_em        timestamptz,
    revogada_em     timestamptz,
    substituida_por uuid
);

create index idx_sessao_usuario on cadastro.sessao_de_renovacao (usuario_id);
create index idx_sessao_expiracao on cadastro.sessao_de_renovacao (expira_em);

create table cadastro.auditoria (
    id          uuid         primary key,
    usuario_id  uuid,
    acao        varchar(60)  not null,
    entidade    varchar(40)  not null,
    entidade_id uuid,
    detalhe     varchar(500),
    correlacao  uuid         not null,
    ocorrido_em timestamptz  not null
);

create index idx_auditoria_ocorrido_em on cadastro.auditoria (ocorrido_em desc);
create index idx_auditoria_entidade on cadastro.auditoria (entidade_id);
create index idx_auditoria_usuario on cadastro.auditoria (usuario_id);

create table cadastro.pedido_de_exclusao (
    id                 uuid        primary key,
    empreendedor_id    uuid        not null unique,
    usuario_id         uuid        not null,
    solicitado_em      timestamptz not null,
    prazo_limite       timestamptz not null,
    concluido_em       timestamptz,
    ultimo_lembrete_em timestamptz
);

create index idx_exclusao_em_andamento
    on cadastro.pedido_de_exclusao (prazo_limite)
    where concluido_em is null;

create table cadastro.confirmacao_de_expurgo (
    pedido_id    uuid        not null references cadastro.pedido_de_exclusao (id),
    participante varchar(30) not null,
    primary key (pedido_id, participante)
);

-- Projecoes alimentadas por evento de outros servicos.

create table cadastro.produto_do_empreendedor (
    produto_id      uuid        primary key,
    empreendedor_id uuid        not null,
    publicado_em    timestamptz not null
);

create index idx_produto_do_empreendedor on cadastro.produto_do_empreendedor (empreendedor_id);

create table cadastro.contato_registrado (
    id              uuid        primary key,
    empreendedor_id uuid        not null,
    produto_id      uuid,
    canal           varchar(20) not null,
    origem          varchar(30) not null,
    ocorrido_em     timestamptz not null
);

create index idx_contato_empreendedor on cadastro.contato_registrado (empreendedor_id);
create index idx_contato_ocorrido_em on cadastro.contato_registrado (ocorrido_em desc);

comment on table cadastro.empreendedor is 'Loja: dados do negocio e situacao na moderacao da SEDECON';
comment on table cadastro.contato_registrado is 'Clique em falar no WhatsApp, sem nada que identifique o consumidor';
