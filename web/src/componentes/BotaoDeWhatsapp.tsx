import { useState } from 'react';
import { avisarContato, BASE_DA_API } from '@/lib/api';

/**
 * O botão que fecha o fluxo do produto.
 *
 * <p>É uma âncora de verdade, com o link montado antes do clique, e não um
 * botão que busca o link e depois abre a janela. A diferença importa: janela
 * aberta por resposta de requisição é bloqueada pelo navegador, e o único
 * caminho de conversão da plataforma morreria em silêncio no celular.
 *
 * <p>O aviso de contato sai em paralelo, sem segurar nada. Se ele falhar, o
 * empreendedor recebe a mensagem do mesmo jeito e a plataforma perde uma
 * estatística, que é a ordem certa de prioridade.
 */
export function BotaoDeWhatsapp({
  empreendedorId,
  telefone,
  nomeDoNegocio,
  produtoId,
  nomeDoProduto,
  origem,
  className = '',
}: {
  empreendedorId: string;
  telefone: string;
  nomeDoNegocio: string;
  produtoId?: string;
  nomeDoProduto?: string;
  origem: 'PAGINA_DO_PRODUTO' | 'PAGINA_DA_LOJA' | 'RESULTADO_DA_BUSCA';
  className?: string;
}) {
  const [avisado, setAvisado] = useState(false);

  const numero = `55${telefone.replace(/\D/g, '')}`;
  const mensagem = nomeDoProduto
    ? `Olá! Vi o ${nomeDoProduto} no Vitrine Bauru e queria saber mais.`
    : `Olá! Vi a sua loja no Vitrine Bauru e queria saber mais.`;
  const link = `https://wa.me/${numero}?text=${encodeURIComponent(mensagem)}`;

  return (
    <a
      href={link}
      target="_blank"
      rel="noopener noreferrer"
      className={`botao botao-selo w-full ${className}`}
      data-base-da-api={BASE_DA_API}
      onClick={() => {
        if (!avisado) {
          avisarContato({ empreendedorId, produtoId, nomeDoProduto, origem });
          setAvisado(true);
        }
      }}
    >
      Falar no WhatsApp
      <span className="sr-only"> com {nomeDoNegocio}</span>
    </a>
  );
}
