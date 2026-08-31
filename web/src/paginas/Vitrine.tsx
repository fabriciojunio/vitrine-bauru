import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
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
      .then((pagina) => {
        definirResultado(pagina);
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
      <section className="bg-papel-fundo border-b-2 border-tinta">
        <div className="max-w-6xl mx-auto px-4 py-10 sm:py-14">
          <h1 className="text-3xl sm:text-5xl max-w-3xl">
            O comércio do seu bairro, sem intermediário.
          </h1>
          <p className="mt-3 text-lg text-tinta-suave max-w-2xl">
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
            <p className="mt-4 text-sm text-tinta-suave">
              <strong>{resumo.lojas}</strong> {resumo.lojas === 1 ? 'loja' : 'lojas'} e{' '}
              <strong>{resumo.produtos}</strong>{' '}
              {resumo.produtos === 1 ? 'produto' : 'produtos'} na vitrine agora.
            </p>
          )}
        </div>
      </section>

      <div className="max-w-6xl mx-auto px-4 py-8">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4 items-end mb-8">
          <Selecao
            etiqueta="Bairro"
            vazio="Todos os bairros"
            opcoes={resumo?.bairros ?? []}
            value={bairro}
            onChange={(evento) => trocarFiltro('bairro', evento.target.value)}
          />
          <Selecao
            etiqueta="Categoria"
            vazio="Todas as categorias"
            opcoes={resumo?.categorias ?? []}
            value={categoria}
            onChange={(evento) => trocarFiltro('categoria', evento.target.value)}
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
            <p className="text-sm text-concreto mb-3">
              {resultado.total} {resultado.total === 1 ? 'resultado' : 'resultados'}
            </p>

            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
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
    </div>
  );
}
