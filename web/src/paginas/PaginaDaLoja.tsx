import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { chamar, ErroDaApi } from '@/lib/api';
import type { LojaCompleta } from '@/lib/tipos';
import { Aviso, Carregando } from '@/componentes/Basicos';
import { CartaoDeProdutoComContato } from '@/componentes/Cartoes';
import { BotaoDeWhatsapp } from '@/componentes/BotaoDeWhatsapp';
import { Foto } from '@/componentes/Foto';

/**
 * A página pública da loja: o endereço que o empreendedor manda para os
 * clientes dele.
 *
 * <p>É por isso que o endereço é o apelido legível, e não o identificador
 * interno: /loja/doces-da-lourdes cabe num cartão de visita impresso, e
 * continua funcionando mesmo que a loja mude de nome depois.
 */
export function PaginaDaLoja() {
  const { apelido } = useParams<{ apelido: string }>();
  const [dados, definirDados] = useState<LojaCompleta | null>(null);
  const [carregando, definirCarregando] = useState(true);
  const [naoExiste, definirNaoExiste] = useState(false);

  useEffect(() => {
    definirCarregando(true);
    definirNaoExiste(false);

    chamar<LojaCompleta>(`/api/busca/lojas/${apelido}`)
      .then(definirDados)
      .catch((falha: ErroDaApi) => {
        if (falha.status === 404) {
          definirNaoExiste(true);
        }
      })
      .finally(() => definirCarregando(false));
  }, [apelido]);

  if (carregando) {
    return <Carregando />;
  }

  if (naoExiste || !dados) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-16">
        <Aviso tipo="atencao" titulo="Essa loja não está disponível">
          Ela pode ter saído da plataforma ou estar em análise pela SEDECON.{' '}
          <Link to="/" className="underline underline-offset-2">
            Voltar para a vitrine
          </Link>
          .
        </Aviso>
      </div>
    );
  }

  const { loja, produtos } = dados;

  return (
    <div>
      <section className="border-b-2 border-tinta bg-papel-fundo">
        <div className="max-w-5xl mx-auto px-4 py-8 grid gap-6 md:grid-cols-[280px_1fr] items-start">
          <div className="quadro carimbo overflow-hidden">
            <Foto
              url={loja.fotoDeCapaUrl}
              nome={loja.nomeDoNegocio}
              categoria={loja.categoria}
              proporcao="aspect-square"
            />
          </div>

          <div>
            <div className="flex flex-wrap items-center gap-2 mb-2">
              <span className="selo-categoria">{loja.categoria}</span>
              <span className="selo-categoria bg-selo-claro">{loja.bairro}</span>
            </div>

            <h1 className="text-3xl sm:text-4xl">{loja.nomeDoNegocio}</h1>

            {loja.descricao && (
              <p className="mt-3 text-lg text-tinta-suave max-w-2xl">{loja.descricao}</p>
            )}

            <div className="mt-5 max-w-sm">
              <BotaoDeWhatsapp
                empreendedorId={loja.id}
                telefone={loja.telefoneWhatsapp}
                nomeDoNegocio={loja.nomeDoNegocio}
                origem="PAGINA_DA_LOJA"
              />
              <p className="text-sm text-concreto mt-2">
                {loja.telefoneWhatsapp} · a conversa acontece direto com o empreendedor
              </p>
            </div>
          </div>
        </div>
      </section>

      <div className="max-w-5xl mx-auto px-4 py-8">
        <h2 className="text-2xl mb-5">
          {produtos.length === 0
            ? 'Esta loja ainda não publicou produtos'
            : `${produtos.length} ${produtos.length === 1 ? 'produto' : 'produtos'} disponíveis`}
        </h2>

        {produtos.length === 0 ? (
          <Aviso tipo="atencao">
            O catálogo ainda está sendo montado. Você pode falar no WhatsApp e perguntar o que
            tem disponível hoje.
          </Aviso>
        ) : (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {produtos.map((produto) => (
              <CartaoDeProdutoComContato
                key={produto.id}
                produto={produto}
                telefone={loja.telefoneWhatsapp}
                nomeDoNegocio={loja.nomeDoNegocio}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
