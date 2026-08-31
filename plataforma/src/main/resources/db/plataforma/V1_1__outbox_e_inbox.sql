-- Tabelas do outbox e do inbox.
--
-- Ficam no esquema padrão, e não num esquema por serviço, de propósito: em
-- implantação separada cada serviço tem o próprio banco e estas tabelas são
-- dele; num processo único, os serviços compartilham a mesma conexão e o mesmo
-- gerenciador de transação, então compartilhar as duas tabelas mantém a
-- garantia do padrão intacta.

create table outbox (
    id                uuid         primary key,
    topico            varchar(120) not null,
    chave             varchar(80)  not null,
    tipo              varchar(120) not null,
    carga             text         not null,
    criada_em         timestamptz  not null,
    publicada_em      timestamptz,
    tentativas        integer      not null default 0,
    proxima_tentativa timestamptz,
    ultimo_erro       varchar(500)
);

-- Índice parcial: a consulta do publicador só olha o que ainda não saiu, e
-- essa fatia é minúscula perto do histórico. Indexar a tabela inteira faria o
-- índice crescer para sempre sem acelerar nada.
create index idx_outbox_pendentes
    on outbox (criada_em)
    where publicada_em is null;

create index idx_outbox_expurgo
    on outbox (publicada_em)
    where publicada_em is not null;

create table inbox (
    evento_id     uuid         not null,
    consumidor    varchar(80)  not null,
    tipo          varchar(120) not null,
    processado_em timestamptz  not null,
    primary key (evento_id, consumidor)
);

create index idx_inbox_processado_em on inbox (processado_em);

comment on table outbox is 'Eventos gravados na mesma transacao do estado, esperando publicacao';
comment on table inbox is 'Marca de evento ja tratado, para o consumidor ser idempotente';
