import { BASE_DA_API } from '@/lib/api';
import { corDaCategoria, iniciais } from '@/lib/formato';

/**
 * A foto do produto, ou o lugar dela.
 *
 * <p>A maioria das lojas começa sem foto: o empreendedor se cadastra pelo
 * celular, publica os produtos e só depois volta para fotografar. Um retângulo
 * cinza vazio faria a vitrine inteira parecer quebrada justamente no dia da
 * inauguração.
 *
 * <p>A ausência de foto vira um bloco na cor da categoria, com a arcada do
 * calçadão desenhada por cima e as iniciais discretas no meio. Fica óbvio que
 * falta foto, sem parecer defeito, e a grade continua com aspecto de coisa
 * cuidada.
 *
 * <p>O tamanho é reservado por proporção e o carregamento é preguiçoso: sem
 * isso, a grade pula enquanto as imagens chegam, o que no celular faz a pessoa
 * clicar no produto errado.
 */
export function Foto({
  url,
  nome,
  categoria,
  proporcao = 'aspect-[3/2]',
}: {
  url: string | null | undefined;
  nome: string;
  categoria?: string | null;
  proporcao?: string;
}) {
  if (!url) {
    return (
      <div
        className={`${proporcao} sem-foto w-full border-b-2 border-tinta`}
        style={{ backgroundColor: corDaCategoria(categoria) }}
        role="img"
        aria-label={`${nome}: foto ainda não enviada`}
      >
        <span className="iniciais-da-foto select-none">{iniciais(nome)}</span>
      </div>
    );
  }

  return (
    <img
      src={url.startsWith('http') ? url : `${BASE_DA_API}${url}`}
      alt={nome}
      loading="lazy"
      decoding="async"
      className={`${proporcao} w-full object-cover border-b-2 border-tinta bg-papel-fundo`}
    />
  );
}
