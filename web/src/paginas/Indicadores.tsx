import { useEffect, useState } from 'react';
import { chamar, ErroDaApi } from '@/lib/api';
import type { PainelDaSedecon } from '@/lib/tipos';
import { formatarData } from '@/lib/formato';
import { Aviso, Carregando, TituloDeSecao } from '@/componentes/Basicos';

/**
 * O painel de impacto da SEDECON.
 *
 * <p>Responde ao objetivo específico número 5 do projeto de extensão, que é
 * avaliar impacto e engajamento. Sem esta tela, a resposta na apresentação
 * final seria impressão pessoal.
 *
 * <p>A métrica que importa é contato iniciado, e não visita: a plataforma não
 * fecha venda, então o que ela consegue provar é que gerou conversa entre
 * consumidor e empreendedor. E a lista de aprovados sem nenhum produto é a
 * mais acionável de todas, porque é a lista de quem precisa de capacitação,
 * que é outro objetivo do projeto.
 */
export function Indicadores() {
  const [painel, definirPainel] = useState<PainelDaSedecon | null>(null);
  const [carregando, definirCarregando] = useState(true);
  const [erro, definirErro] = useState<string | null>(null);

  useEffect(() => {
    chamar<PainelDaSedecon>('/api/cadastro/moderacao/indicadores')
      .then(definirPainel)
      .catch((falha: ErroDaApi) => definirErro(falha.message))
      .finally(() => definirCarregando(false));
  }, []);

  if (carregando) {
    return <Carregando />;
  }

  if (erro || !painel) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <Aviso tipo="erro">{erro ?? 'Não foi possível carregar os indicadores.'}</Aviso>
      </div>
    );
  }

  const bairros = Object.entries(painel.aprovadosPorBairro);
  const maiorBairro = Math.max(1, ...bairros.map(([, quantidade]) => quantidade));

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <TituloDeSecao descricao="O que a plataforma produziu até agora, em números que dá para conferir.">
        Impacto e engajamento
      </TituloDeSecao>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Cartao titulo="Lojas no ar" valor={painel.empreendedoresAprovados} destaque />
        <Cartao titulo="Esperando análise" valor={painel.cadastrosPendentes} />
        <Cartao titulo="Produtos publicados" valor={painel.produtosPublicados} />
        <Cartao
          titulo="Contatos em 30 dias"
          valor={painel.contatosNosUltimos30Dias}
          rodape={`${painel.contatosNoTotal} no total`}
          destaque
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2 mt-10">
        <section className="quadro p-5">
          <h3 className="font-display text-xl font-bold mb-1">Lojas por bairro</h3>
          <p className="text-sm text-concreto mb-4">
            Mostra onde a plataforma pegou e, principalmente, onde ainda não chegou.
          </p>

          {bairros.length === 0 ? (
            <p className="text-tinta-suave">Nenhuma loja aprovada ainda.</p>
          ) : (
            <ul className="flex flex-col gap-2">
              {bairros.map(([bairro, quantidade]) => (
                <li key={bairro} className="flex items-center gap-3">
                  <span className="text-sm w-44 shrink-0 truncate" title={bairro}>
                    {bairro}
                  </span>
                  {/* Barra desenhada com div, e não com biblioteca de gráfico:
                      é uma barra, e uma dependência a mais pesaria mais que o
                      problema que resolve. */}
                  <span
                    className="h-4 bg-sinal border border-borda-forte"
                    style={{ width: `${Math.round((quantidade / maiorBairro) * 70)}%` }}
                    aria-hidden="true"
                  />
                  <span className="text-sm font-semibold">{quantidade}</span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="quadro p-5">
          <h3 className="font-display text-xl font-bold mb-1">Quem mais recebe contato</h3>
          <p className="text-sm text-concreto mb-4">Últimos 30 dias.</p>

          {painel.maisProcurados.length === 0 ? (
            <p className="text-tinta-suave">Nenhum contato registrado ainda.</p>
          ) : (
            <ol className="flex flex-col gap-2">
              {painel.maisProcurados.map((loja, posicao) => (
                <li key={loja.empreendedorId} className="flex items-baseline gap-3">
                  <span className="font-display text-2xl font-bold text-concreto w-8">
                    {posicao + 1}
                  </span>
                  <span className="grow">{loja.nomeDoNegocio}</span>
                  <span className="preco text-sm">{loja.contatos}</span>
                </li>
              ))}
            </ol>
          )}
        </section>
      </div>

      <section className="quadro p-5 mt-6 bg-sinal-claro">
        <h3 className="font-display text-xl font-bold mb-1">
          Aprovados que ainda não publicaram nada ({painel.aprovadosSemNenhumProduto})
        </h3>
        <p className="text-sm text-tinta-suave mb-4">
          Esta é a lista de capacitação: são empreendedores que passaram pela análise e não
          conseguiram, ou não souberam, montar o catálogo. Vale uma ligação da Casa do
          Empreendedor.
        </p>

        {painel.precisamDeAjuda.length === 0 ? (
          <p className="text-tinta-suave">
            Todo mundo que foi aprovado já publicou pelo menos um produto.
          </p>
        ) : (
          <ul className="flex flex-col gap-2">
            {painel.precisamDeAjuda.map((loja) => (
              <li
                key={loja.empreendedorId}
                className="flex flex-wrap items-baseline justify-between gap-2 border-b border-dashed border-linha pb-2"
              >
                <span className="font-semibold">{loja.nomeDoNegocio}</span>
                <span className="text-sm text-concreto">
                  {loja.bairro} · aprovado em {formatarData(loja.aprovadoEm)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <div className="grid gap-4 sm:grid-cols-2 mt-6">
        <Cartao titulo="Cadastros recusados" valor={painel.cadastrosRejeitados} />
        <Cartao titulo="Lojas suspensas" valor={painel.empreendedoresSuspensos} />
      </div>
    </div>
  );
}

function Cartao({
  titulo,
  valor,
  rodape,
  destaque = false,
}: {
  titulo: string;
  valor: number;
  rodape?: string;
  destaque?: boolean;
}) {
  return (
    <div className={`quadro p-4 ${destaque ? 'placa bg-selo-claro' : 'placa-leve'}`}>
      <p className="font-display text-4xl font-bold">{valor}</p>
      <p className="text-sm text-tinta-suave mt-1">{titulo}</p>
      {rodape && <p className="text-xs text-concreto mt-1">{rodape}</p>}
    </div>
  );
}
