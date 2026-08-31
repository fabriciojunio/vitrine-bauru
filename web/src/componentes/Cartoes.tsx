import { Link } from 'react-router-dom';
import { Foto } from './Foto';
import { BotaoDeWhatsapp } from './BotaoDeWhatsapp';
import type { LojaNaVitrine, ProdutoNaVitrine } from '@/lib/tipos';

/**
 * O produto na grade da vitrine.
 *
 * <p>A hierarquia é foto, nome, preço, loja, nessa ordem, e o tamanho de cada
 * um reflete isso. Quem procura no celular decide pela imagem e pelo preço; o
 * nome da loja importa depois, na hora de confiar.
 *
 * <p>A categoria fica sobre a foto, e não numa linha própria: economiza altura
 * sem esconder informação, e a grade fica com cartões do mesmo tamanho, o que
 * é o que faz uma vitrine parecer organizada.
 */
export function CartaoDeProduto({ produto }: { produto: ProdutoNaVitrine }) {
  return (
    <article className="quadro overflow-hidden flex flex-col">
      <div className="relative">
        <Foto url={produto.imagemUrl} nome={produto.nome} categoria={produto.categoria} />
        <span className="selo-categoria absolute left-2 top-2 bg-chapa">
          {produto.categoria}
        </span>
      </div>

      <div className="p-3.5 flex flex-col gap-2 grow">
        <h3 className="text-lg leading-snug">{produto.nome}</h3>

        {produto.descricao && (
          <p className="text-sm text-tinta-suave duas-linhas">{produto.descricao}</p>
        )}

        <p
          className={`self-start mt-auto ${
            produto.precoEmCentavos === null ? 'preco preco-sob-consulta' : 'preco'
          }`}
        >
          {produto.precoFormatado}
        </p>

        <div className="pt-2.5 mt-1 border-t-2 border-dashed border-linha flex items-baseline justify-between gap-2">
          {produto.lojaApelido ? (
            <Link
              to={`/loja/${produto.lojaApelido}`}
              className="text-sm font-semibold underline underline-offset-2 truncate"
            >
              {produto.lojaNome}
            </Link>
          ) : (
            <span className="text-sm font-semibold truncate">{produto.lojaNome}</span>
          )}
          {produto.bairro && (
            <span className="text-xs text-concreto shrink-0">{produto.bairro}</span>
          )}
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
    <article className="quadro overflow-hidden flex flex-col">
      <div className="relative">
        <Foto url={produto.imagemUrl} nome={produto.nome} categoria={produto.categoria} />
        <span className="selo-categoria absolute left-2 top-2 bg-chapa">
          {produto.categoria}
        </span>
      </div>

      <div className="p-3.5 flex flex-col gap-2 grow">
        <h3 className="text-lg leading-snug">{produto.nome}</h3>

        {produto.descricao && (
          <p className="text-sm text-tinta-suave tres-linhas">{produto.descricao}</p>
        )}

        <p
          className={`self-start mt-auto ${
            produto.precoEmCentavos === null ? 'preco preco-sob-consulta' : 'preco'
          }`}
        >
          {produto.precoFormatado}
        </p>

        <div className="pt-2.5">
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
    <article className="quadro overflow-hidden flex flex-col">
      <div className="relative">
        <Foto
          url={loja.fotoDeCapaUrl}
          nome={loja.nomeDoNegocio}
          categoria={loja.categoria}
          proporcao="aspect-[16/9]"
          alturaSemFoto="h-20"
        />
        {/* As duas etiquetas dividem a mesma linha. Soltas nos cantos, uma
            categoria longa com um bairro longo se atropelavam. */}
        <div className="absolute left-2 right-2 top-2 flex items-start justify-between gap-2">
          <span className="selo-categoria bg-chapa truncate max-w-[58%]">{loja.categoria}</span>
          <span className="selo-categoria bg-selo-claro truncate max-w-[58%]">{loja.bairro}</span>
        </div>
      </div>

      <div className="p-3.5 flex flex-col gap-2 grow">
        <h3 className="text-xl leading-snug">
          <Link to={`/loja/${loja.apelidoNaUrl}`} className="underline underline-offset-2">
            {loja.nomeDoNegocio}
          </Link>
        </h3>

        {loja.descricao && (
          <p className="text-sm text-tinta-suave tres-linhas">{loja.descricao}</p>
        )}

        <div className="mt-auto pt-2.5">
          <Link to={`/loja/${loja.apelidoNaUrl}`} className="botao botao-neutro w-full">
            Ver a loja
          </Link>
        </div>
      </div>
    </article>
  );
}
