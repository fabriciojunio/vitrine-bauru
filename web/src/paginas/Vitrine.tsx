import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { chamar, ErroDaApi } from '@/lib/api';
import type { Pagina, ProdutoNaVitrine, ResumoDaVitrine } from '@/lib/tipos';
import { Aviso, Botao, Carregando, Selecao, TituloDeSecao } from '@/componentes/Basicos';
import { CartaoDeProduto } from '@/componentes/Cartoes';
import { ApiDesligada } from '@/componentes/ApiDesligada';

/**
 * A página que o consumidor abre.
 *
 * <p>Mostra a vitrine inteira antes de pedir qualquer coisa. Tela que começa
 * com um campo de busca vazio e nada embaixo é a forma mais rápida de perder
 * quem chegou sem saber o que procurar, que é a maioria de quem entra pela
 * primeira vez.
 *
 * <p>Os filtros ficam na URL. Isso não é detalhe técnico: é o que permite ao
 * empreendedor mandar no grupo do bairro um link que já abre a busca certa, e
 * ao consumidor voltar pelo botão do navegador sem perder o que filtrou.
 */
export function Vitrine() {
  const [parametros, definirParametros] = useSearchParams();
  const [resultado, definirResultado] = useState<Pagina<ProdutoNaVitrine> | null>(null);
  const [resumo, definirResumo] = useState<ResumoDaVitrine | null>(null);
  const [carregando, definirCarregando] = useState(true);
  const [erro, definirErro] = useState<string | null>(null);
  const [semConexao, definirSemConexao] = useState(false);

  const termo = parametros.get('termo') ?? '';
  const bairro = parametros.get('bairro') ?? '';
  const categoria = parametros.get('categoria') ?? '';
  const pagina = Number(parametros.get('pagina') ?? '0');

  const [textoDigitado, definirTextoDigitado] = useState(termo);

  useEffect(() => {
    chamar<ResumoDaVitrine>('/api/busca/resumo')
      .then(definirResumo)
      .catch(() => definirResumo(null));
  }, []);

  useEffect(() => {
    definirCarregando(true);
    definirErro(null);

    const consulta = new URLSearchParams();
    if (termo) consulta.set('termo', termo);
    if (bairro) consulta.set('bairro', bairro);
    if (categoria) consulta.set('categoria', categoria);
    consulta.set('pagina', String(pagina));
    consulta.set('tamanho', '24');

    chamar<Pagina<ProdutoNaVitrine>>(`/api/busca/produtos?${consulta}`)
      .then((paginaRecebida) => {
        definirResultado(paginaRecebida);
        definirSemConexao(false);
      })
      .catch((falha: ErroDaApi) => {
        if (falha.ehDeConexao) {
          definirSemConexao(true);
        } else {
          definirErro(falha.message);
        }
      })
      .finally(() => definirCarregando(false));
  }, [termo, bairro, categoria, pagina]);

  const trocarFiltro = useCallback(
    (chave: string, valor: string) => {
      const novos = new URLSearchParams(parametros);
      if (valor) {
        novos.set(chave, valor);
      } else {
        novos.delete(chave);
      }
      // Trocar filtro sempre volta para a primeira página: manter a página
      // sete depois de filtrar deixa a tela vazia sem explicação.
      novos.delete('pagina');
      definirParametros(novos);
    },
    [parametros, definirParametros],
  );

  const temFiltro = Boolean(termo || bairro || categoria);

  if (semConexao) {
    return <ApiDesligada aoTentarDeNovo={() => window.location.reload()} />;
  }

  return (
    <div>
      {/*
        O herói é verde institucional com a arcada do calçadão desenhada por
        baixo. É o que faz a página ler como serviço da prefeitura no primeiro
        segundo, antes de qualquer texto.
      */}
      <section className="bg-selo text-papel-claro border-b-4 border-tinta relative overflow-hidden">
        <div className="arcada absolute inset-x-0 bottom-0 h-32" aria-hidden="true" />

        <div className="max-w-6xl mx-auto px-4 py-10 sm:py-14 relative">
          <p className="selo-categoria selo-no-verde mb-4">Uma iniciativa da SEDECON Bauru</p>

          <h1 className="text-3xl sm:text-5xl max-w-3xl">
            O comércio do seu bairro, sem intermediário.
          </h1>

          <p className="mt-3 text-lg text-papel/90 max-w-2xl">
            Encontre quem produz e presta serviço em Bauru e fale direto no WhatsApp. Sem taxa,
            sem cadastro para comprar, sem carrinho.
          </p>

          <form
            className="mt-6 flex flex-col sm:flex-row gap-3 max-w-3xl"
            onSubmit={(evento) => {
              evento.preventDefault();
              trocarFiltro('termo', textoDigitado.trim());
            }}
            role="search"
          >
            <label htmlFor="busca" className="sr-only">
              O que você procura
            </label>
            <input
              id="busca"
              name="termo"
              className="campo grow"
              placeholder="Bolo de pote, conserto de máquina, marmita…"
              value={textoDigitado}
              onChange={(evento) => definirTextoDigitado(evento.target.value)}
            />
            <Botao type="submit">Procurar</Botao>
          </form>

          {resumo && (
            <dl className="mt-7 flex flex-wrap gap-x-10 gap-y-4">
              <div>
                <dt className="text-xs uppercase tracking-wider text-papel/70">Lojas no ar</dt>
                <dd className="font-display text-3xl font-bold">{resumo.lojas}</dd>
              </div>
              <div>
                <dt className="text-xs uppercase tracking-wider text-papel/70">
                  Produtos e serviços
                </dt>
                <dd className="font-display text-3xl font-bold">{resumo.produtos}</dd>
              </div>
              <div>
                <dt className="text-xs uppercase tracking-wider text-papel/70">
                  Bairros atendidos
                </dt>
                <dd className="font-display text-3xl font-bold">{resumo.bairros.length}</dd>
              </div>
            </dl>
          )}
        </div>
      </section>

      {/* Atalhos por categoria: quem chegou sem saber o que procurar precisa de
          um ponto de partida, e não de um campo de busca vazio. */}
      {resumo && resumo.categorias.length > 0 && (
        <nav
          className="bg-papel-fundo border-b-2 border-linha"
          aria-label="Categorias em destaque"
        >
          <div className="max-w-6xl mx-auto px-4 py-3 flex gap-2 overflow-x-auto">
            <button
              className={`selo-categoria py-1.5 ${categoria === '' ? 'bg-mostarda' : ''}`}
              onClick={() => trocarFiltro('categoria', '')}
            >
              Tudo
            </button>
            {resumo.categorias.map((nome) => (
              <button
                key={nome}
                className={`selo-categoria py-1.5 ${categoria === nome ? 'bg-mostarda' : ''}`}
                onClick={() => trocarFiltro('categoria', nome)}
              >
                {nome}
              </button>
            ))}
          </div>
        </nav>
      )}

      <div className="max-w-6xl mx-auto px-4 py-8">
        {/* A categoria já tem os atalhos acima; aqui fica só o bairro, que é o
            filtro que o consumidor mais usa e que não cabe numa fila de
            botões, porque Bauru tem dezenas deles. */}
        <div className="grid gap-3 sm:grid-cols-[minmax(0,18rem)_auto] items-end mb-8">
          <Selecao
            etiqueta="Bairro"
            vazio="Todos os bairros"
            opcoes={resumo?.bairros ?? []}
            value={bairro}
            onChange={(evento) => trocarFiltro('bairro', evento.target.value)}
          />

          {temFiltro && (
            <Botao
              variante="neutro"
              onClick={() => {
                definirTextoDigitado('');
                definirParametros(new URLSearchParams());
              }}
            >
              Limpar filtros
            </Botao>
          )}
        </div>

        <TituloDeSecao
          descricao={
            temFiltro
              ? 'Resultado da sua busca. Clique na loja para ver o catálogo completo.'
              : 'Tudo que está disponível agora, do mais recente para o mais antigo.'
          }
        >
          {temFiltro ? 'O que encontramos' : 'Na vitrine hoje'}
        </TituloDeSecao>

        {erro && (
          <Aviso tipo="erro" titulo="Não deu para carregar a vitrine">
            {erro} Tente atualizar a página em alguns instantes.
          </Aviso>
        )}

        {carregando && <Carregando texto="Procurando…" />}

        {!carregando && resultado && resultado.conteudo.length === 0 && (
          <Aviso tipo="atencao" titulo="Nada encontrado com esses filtros">
            Tente uma palavra mais curta, ou tire o filtro de bairro. Se você procura algo que
            ainda não existe na plataforma, avise a Casa do Empreendedor: a SEDECON usa isso para
            convidar novos negócios.
          </Aviso>
        )}

        {!carregando && resultado && resultado.conteudo.length > 0 && (
          <>
            <p className="text-sm text-concreto mb-4">
              {resultado.total} {resultado.total === 1 ? 'resultado' : 'resultados'}
            </p>

            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {resultado.conteudo.map((produto) => (
                <CartaoDeProduto key={produto.id} produto={produto} />
              ))}
            </div>

            {resultado.totalDePaginas > 1 && (
              <nav className="flex items-center justify-between gap-3 mt-8" aria-label="Paginação">
                <Botao
                  variante="neutro"
                  disabled={pagina === 0}
                  onClick={() => trocarFiltro('pagina', String(pagina - 1))}
                >
                  Anterior
                </Botao>
                <span className="text-sm text-concreto">
                  Página {pagina + 1} de {resultado.totalDePaginas}
                </span>
                <Botao
                  variante="neutro"
                  disabled={!resultado.temProxima}
                  onClick={() => trocarFiltro('pagina', String(pagina + 1))}
                >
                  Próxima
                </Botao>
              </nav>
            )}
          </>
        )}
      </div>

      <ComoFunciona />
    </div>
  );
}

/**
 * Explica o produto em três passos, no fim da página.
 *
 * <p>Fica depois da vitrine, e não antes: quem chega quer ver o que tem, e não
 * ler como funciona. Mas quem rolou até o fim é justamente quem está decidindo
 * se confia, e é para essa pessoa que este bloco existe.
 */
function ComoFunciona() {
  const passos = [
    {
      numero: '1',
      titulo: 'Você encontra',
      texto:
        'Procure por produto, por bairro ou por categoria. Não precisa criar conta nem informar nada.',
    },
    {
      numero: '2',
      titulo: 'Fala direto com quem faz',
      texto:
        'O botão abre uma conversa no WhatsApp do empreendedor, com a mensagem já escrita.',
    },
    {
      numero: '3',
      titulo: 'Combina como preferir',
      texto:
        'Preço, entrega e pagamento são combinados entre vocês dois. A plataforma não cobra nada de ninguém.',
    },
  ];

  return (
    <section className="bg-papel-fundo border-y-2 border-linha arcada-tinta">
      <div className="max-w-6xl mx-auto px-4 py-12">
        <TituloDeSecao descricao="A plataforma aproxima e sai do caminho.">
          Como funciona
        </TituloDeSecao>

        <div className="grid gap-6 sm:grid-cols-3">
          {passos.map((passo) => (
            <div key={passo.numero} className="quadro carimbo-leve p-5">
              <span className="font-display text-4xl font-bold text-terracota leading-none">
                {passo.numero}
              </span>
              <h3 className="text-xl mt-2">{passo.titulo}</h3>
              <p className="text-sm text-tinta-suave mt-1.5 leading-relaxed">{passo.texto}</p>
            </div>
          ))}
        </div>

        <div className="quadro carimbo p-6 mt-8 bg-selo text-papel-claro flex flex-col sm:flex-row sm:items-center gap-4 justify-between">
          <div>
            <h3 className="text-2xl text-papel-claro">Você tem um negócio em Bauru?</h3>
            <p className="text-papel/90 mt-1">
              O cadastro é de graça. A SEDECON confere os dados e sua loja entra na vitrine.
            </p>
          </div>
          <Link to="/cadastrar" className="botao botao-principal shrink-0">
            Cadastrar meu negócio
          </Link>
        </div>
      </div>
    </section>
  );
}
