import { Link } from 'react-router-dom';
import { Foto } from './Foto';
import { BotaoDeWhatsapp } from './BotaoDeWhatsapp';
import type { LojaNaVitrine, ProdutoNaVitrine } from '@/lib/tipos';

/**
 * O produto na grade da vitrine.
 *
 * A foto ocupa a maior parte do cartão de propósito: quem procura no celular
 * decide pela imagem, e espremer a foto para caber mais texto é o erro clássico
 * de vitrine feita por quem programa e não por quem vende.
 */
export function CartaoDeProduto({ produto }: { produto: ProdutoNaVitrine }) {
  return (
    <article className="quadro carimbo overflow-hidden flex flex-col">
      <Foto url={produto.imagemUrl} nome={produto.nome} categoria={produto.categoria} />

      <div className="p-3 flex flex-col gap-2 grow">
        <div className="flex items-start justify-between gap-2">
          <h3 className="text-lg leading-snug">{produto.nome}</h3>
        </div>

        {produto.descricao && (
          <p className="text-sm text-tinta-suave line-clamp-3">{produto.descricao}</p>
        )}

        <p className="preco self-start">{produto.precoFormatado}</p>

        <div className="mt-auto pt-2 border-t-2 border-dashed border-linha">
          {produto.lojaApelido ? (
            <Link
              to={`/loja/${produto.lojaApelido}`}
              className="text-sm font-semibold underline underline-offset-2"
            >
              {produto.lojaNome}
            </Link>
          ) : (
            <span className="text-sm font-semibold">{produto.lojaNome}</span>
          )}
          {produto.bairro && <p className="text-xs text-concreto">{produto.bairro}</p>}
        </div>
      </div>
    </article>
  );
}

/** O mesmo cartão, com o botão de contato: usado na página da loja. */
export function CartaoDeProdutoComContato({
  produto,
  telefone,
  nomeDoNegocio,
}: {
  produto: ProdutoNaVitrine;
  telefone: string;
  nomeDoNegocio: string;
}) {
  return (
    <article className="quadro carimbo overflow-hidden flex flex-col">
      <Foto url={produto.imagemUrl} nome={produto.nome} categoria={produto.categoria} />

      <div className="p-3 flex flex-col gap-2 grow">
        <h3 className="text-lg leading-snug">{produto.nome}</h3>
        {produto.descricao && (
          <p className="text-sm text-tinta-suave line-clamp-3">{produto.descricao}</p>
        )}
        <p className="preco self-start">{produto.precoFormatado}</p>

        <div className="mt-auto pt-2">
          <BotaoDeWhatsapp
            empreendedorId={produto.empreendedorId}
            telefone={telefone}
            nomeDoNegocio={nomeDoNegocio}
            produtoId={produto.id}
            nomeDoProduto={produto.nome}
            origem="PAGINA_DO_PRODUTO"
          />
        </div>
      </div>
    </article>
  );
}

export function CartaoDeLoja({ loja }: { loja: LojaNaVitrine }) {
  return (
    <article className="quadro carimbo overflow-hidden flex flex-col">
      <Foto
        url={loja.fotoDeCapaUrl}
        nome={loja.nomeDoNegocio}
        categoria={loja.categoria}
        proporcao="aspect-[16/9]"
      />

      <div className="p-3 flex flex-col gap-2 grow">
        <div className="flex flex-wrap items-center gap-2">
          <span className="selo-categoria">{loja.categoria}</span>
          <span className="text-xs text-concreto">{loja.bairro}</span>
        </div>

        <h3 className="text-xl leading-snug">
          <Link to={`/loja/${loja.apelidoNaUrl}`} className="underline underline-offset-2">
            {loja.nomeDoNegocio}
          </Link>
        </h3>

        {loja.descricao && (
          <p className="text-sm text-tinta-suave line-clamp-3">{loja.descricao}</p>
        )}

        <div className="mt-auto pt-2">
          <Link to={`/loja/${loja.apelidoNaUrl}`} className="botao botao-neutro w-full">
            Ver a loja
          </Link>
        </div>
      </div>
    </article>
  );
}
