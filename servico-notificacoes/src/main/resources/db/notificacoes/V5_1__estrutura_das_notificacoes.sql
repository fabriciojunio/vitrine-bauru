-- Estrutura do serviço de notificações.

create schema if not exists notificacoes;

create table notificacoes.notificacao (
    id                uuid         primary key,
    empreendedor_id   uuid         not null,
    destinatario      varchar(160) not null,
    assunto           varchar(160) not null,
    corpo             text         not null,
    tipo              varchar(30)  not null,
    criada_em         timestamptz  not null,
    enviada_em        timestamptz,
    tentativas        integer      not null default 0,
    proxima_tentativa timestamptz,
    ultimo_erro       varchar(400)
);

-- A consulta do enviador só olha o que ainda não saiu.
create index idx_notificacao_pendente
    on notificacoes.notificacao (criada_em)
    where enviada_em is null;

create index idx_notificacao_empreendedor on notificacoes.notificacao (empreendedor_id);

comment on column notificacoes.notificacao.id is
    'E o id do evento que originou a mensagem: reentrega do broker nao gera segundo e-mail';
comment on column notificacoes.notificacao.corpo is
    'Guardado para conseguir responder o que exatamente foi enviado quando alguem disser que nao recebeu';
