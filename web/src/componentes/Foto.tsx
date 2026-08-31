import { BASE_DA_API } from '@/lib/api';
import { corDaCategoria, iniciais } from '@/lib/formato';

/**
 * A foto do produto, ou o lugar dela.
 *
 * <p>A maioria das lojas começa sem foto: o empreendedor se cadastra pelo
 * celular, publica os produtos e só depois volta para fotografar. Numa cidade
 * recém-cadastrada isso significa uma vitrine quase toda sem imagem.
 *
 * <p>Por isso o lugar vazio não ocupa o tamanho de uma foto. Sem imagem, o
 * bloco vira uma faixa fina na cor da categoria, com a arcada por cima e as
 * iniciais no meio: o cartão continua reconhecível na grade, mas quem rola a
 * página lê nome e preço em vez de atravessar dezenas de retângulos coloridos.
 * Uma faixa de 56px no lugar de 240px é a diferença entre um catálogo e um
 * mostruário de cores.
 *
 * <p>Quando a foto existe ela recebe o espaço inteiro, porque aí ela é a
 * informação principal. O tamanho é reservado por proporção e o carregamento é
 * preguiçoso: sem isso a grade pula enquanto as imagens chegam, o que no
 * celular faz a pessoa clicar no produto errado.
 */
export function Foto({
  url,
  nome,
  categoria,
  proporcao = 'aspect-[3/2]',
  alturaSemFoto = 'h-14',
}: {
  url: string | null | undefined;
  nome: string;
  categoria?: string | null;
  proporcao?: string;
  alturaSemFoto?: string;
}) {
  if (!url) {
    return (
      <div
        className={`${alturaSemFoto} sem-foto w-full border-b border-borda`}
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
      className={`${proporcao} w-full object-cover border-b border-borda bg-faixa`}
    />
  );
}
