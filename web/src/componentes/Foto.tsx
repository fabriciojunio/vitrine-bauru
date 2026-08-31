import { BASE_DA_API } from '@/lib/api';
import { corDaCategoria, iniciais } from '@/lib/formato';

/**
 * A foto do produto, ou o lugar dela.
 *
 * A maioria das lojas começa sem foto: o empreendedor se cadastra pelo
 * celular, publica os produtos e só depois volta para fotografar. Um retângulo
 * cinza vazio faria a vitrine inteira parecer quebrada justamente no dia da
 * inauguração, então a ausência de foto vira um bloco de cor da categoria com
 * as iniciais do produto. Fica óbvio que falta foto, sem parecer defeito.
 *
 * O carregamento é preguiçoso e o tamanho é reservado por proporção: sem isso,
 * a grade pula enquanto as imagens chegam, o que no celular faz a pessoa
 * clicar no produto errado.
 */
export function Foto({
  url,
  nome,
  categoria,
  proporcao = 'aspect-[4/3]',
  arco = true,
}: {
  url: string | null | undefined;
  nome: string;
  categoria?: string | null;
  proporcao?: string;
  arco?: boolean;
}) {
  const formato = arco ? 'arco' : '';

  if (!url) {
    return (
      <div
        className={`${proporcao} ${formato} w-full flex items-center justify-center border-b-2 border-tinta`}
        style={{ backgroundColor: corDaCategoria(categoria) }}
        role="img"
        aria-label={`${nome}: foto ainda não enviada`}
      >
        <span className="font-display text-4xl font-bold text-tinta/45 select-none">
          {iniciais(nome)}
        </span>
      </div>
    );
  }

  return (
    <img
      src={url.startsWith('http') ? url : `${BASE_DA_API}${url}`}
      alt={nome}
      loading="lazy"
      decoding="async"
      className={`${proporcao} ${formato} w-full object-cover border-b-2 border-tinta bg-papel-fundo`}
    />
  );
}
