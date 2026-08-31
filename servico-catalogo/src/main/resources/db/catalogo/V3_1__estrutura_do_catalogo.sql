-- Estrutura do serviço de catálogo.

create schema if not exists catalogo;

create table catalogo.categoria (
    id    uuid        primary key,
    nome  varchar(60) not null unique,
    slug  varchar(60) not null unique,
    ordem integer     not null
);

-- Os identificadores são fixos porque o evento de produto carrega o id da
-- categoria, e ele precisa ser o mesmo em qualquer ambiente. Categoria com id
-- sorteado a cada implantação faria a projeção da busca apontar para o vazio
-- depois de uma reinstalação do banco.
insert into catalogo.categoria (id, nome, slug, ordem) values
    ('c0000000-0000-4000-8000-000000000001', 'Alimentação',        'alimentacao',        1),
    ('c0000000-0000-4000-8000-000000000002', 'Artesanato',         'artesanato',         2),
    ('c0000000-0000-4000-8000-000000000003', 'Beleza e bem-estar', 'beleza-e-bem-estar', 3),
    ('c0000000-0000-4000-8000-000000000004', 'Casa e construção',  'casa-e-construcao',  4),
    ('c0000000-0000-4000-8000-000000000005', 'Moda e acessórios',  'moda-e-acessorios',  5),
    ('c0000000-0000-4000-8000-000000000006', 'Serviços gerais',    'servicos-gerais',    6),
    ('c0000000-0000-4000-8000-000000000007', 'Assistência técnica','assistencia-tecnica',7),
    ('c0000000-0000-4000-8000-000000000008', 'Educação e aulas',   'educacao-e-aulas',   8),
    ('c0000000-0000-4000-8000-000000000009', 'Pet',                'pet',                9),
    ('c0000000-0000-4000-8000-000000000010', 'Saúde',              'saude',             10),
    ('c0000000-0000-4000-8000-000000000011', 'Eventos e festas',   'eventos-e-festas',  11),
    ('c0000000-0000-4000-8000-000000000012', 'Automotivo',         'automotivo',        12);

create table catalogo.imagem (
    id              uuid        primary key,
    empreendedor_id uuid        not null,
    tipo            varchar(10) not null,
    tamanho         integer     not null,
    conteudo        bytea       not null,
    criada_em       timestamptz not null
);

create index idx_imagem_empreendedor on catalogo.imagem (empreendedor_id);

create table catalogo.produto (
    id                uuid         primary key,
    empreendedor_id   uuid         not null,
    nome              varchar(120) not null,
    descricao         varchar(800),
    preco_em_centavos bigint,
    categoria_id      uuid         not null references catalogo.categoria (id),
    imagem_id         uuid         references catalogo.imagem (id),
    disponivel        boolean      not null default true,
    criado_em         timestamptz  not null,
    atualizado_em     timestamptz  not null,
    retirado_em       timestamptz
);

-- Consulta do painel do empreendedor: os produtos vivos de uma loja.
create index idx_produto_da_loja
    on catalogo.produto (empreendedor_id, criado_em desc)
    where retirado_em is null;

create index idx_produto_categoria on catalogo.produto (categoria_id);

-- Preço negativo não existe. A trava fica também no banco porque regra que só
-- existe na aplicação some no dia em que alguém corrige dado por SQL.
alter table catalogo.produto
    add constraint preco_nao_negativo check (preco_em_centavos is null or preco_em_centavos >= 0);

create table catalogo.empreendedor_conhecido (
    id              uuid         primary key,
    nome_do_negocio varchar(120) not null,
    pode_publicar   boolean      not null,
    atualizado_em   timestamptz  not null
);

comment on table catalogo.empreendedor_conhecido is
    'Copia minima do cadastro, mantida por evento, para o catalogo nao depender do outro servico no ar';
comment on column catalogo.produto.preco_em_centavos is
    'Nulo significa sob consulta; zero e um preco valido';
