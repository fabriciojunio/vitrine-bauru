import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { chamar, ErroDaApi } from '@/lib/api';
import type { CategoriaDoCatalogo, Pagina, ProdutoDoPainel } from '@/lib/tipos';
import { centavosParaCampo, paraCentavos } from '@/lib/formato';
import {
  AreaDeTexto,
  Aviso,
  Botao,
  Campo,
  Carregando,
  Selecao,
  TituloDeSecao,
} from '@/componentes/Basicos';
import { Foto } from '@/componentes/Foto';

/**
 * O catálogo, do ponto de vista de quem vende.
 *
 * <p>Três decisões que vieram de pensar em quem usa isso pelo celular, no
 * intervalo do trabalho:
 *
 * <ul>
 *   <li>o preço aceita "12,50" e "12.50", e vazio significa "sob consulta", que
 *       é a resposta certa para serviço sob medida;</li>
 *   <li>marcar como esgotado fica ao lado do produto, com um clique, porque é a
 *       ação mais frequente do dia a dia;</li>
 *   <li>a foto sobe depois, sozinha, e uma falha nela não derruba o produto que
 *       acabou de ser cadastrado.</li>
 * </ul>
 */
export function MeusProdutos() {
  const [produtos, definirProdutos] = useState<ProdutoDoPainel[]>([]);
  const [categorias, definirCategorias] = useState<CategoriaDoCatalogo[]>([]);
  const [carregando, definirCarregando] = useState(true);
  const [erro, definirErro] = useState<string | null>(null);
  const [emEdicao, definirEmEdicao] = useState<ProdutoDoPainel | 'novo' | null>(null);

  const recarregar = useCallback(() => {
    definirCarregando(true);
    chamar<Pagina<ProdutoDoPainel>>('/api/catalogo/meus-produtos?tamanho=60')
      .then((pagina) => definirProdutos(pagina.conteudo))
      .catch((falha: ErroDaApi) => definirErro(falha.message))
      .finally(() => definirCarregando(false));
  }, []);

  useEffect(() => {
    recarregar();
    chamar<CategoriaDoCatalogo[]>('/api/catalogo/categorias')
      .then(definirCategorias)
      .catch(() => definirCategorias([]));
  }, [recarregar]);

  const alternarDisponibilidade = async (produto: ProdutoDoPainel) => {
    // Troca na tela antes da resposta: o botão precisa parecer instantâneo
    // para quem está com internet ruim. Se falhar, a recarga desfaz.
    definirProdutos((atuais) =>
      atuais.map((item) =>
        item.id === produto.id ? { ...item, disponivel: !item.disponivel } : item,
      ),
    );

    try {
      await chamar(
        `/api/catalogo/meus-produtos/${produto.id}/disponibilidade?disponivel=${!produto.disponivel}`,
        { metodo: 'PUT' },
      );
    } catch {
      recarregar();
    }
  };

  const retirar = async (produto: ProdutoDoPainel) => {
    if (!window.confirm(`Retirar "${produto.nome}" do catálogo? Isso não pode ser desfeito.`)) {
      return;
    }
    try {
      await chamar(`/api/catalogo/meus-produtos/${produto.id}`, { metodo: 'DELETE' });
      recarregar();
    } catch (falha) {
      definirErro(falha instanceof ErroDaApi ? falha.message : 'Não foi possível retirar.');
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <Link to="/painel" className="botao botao-texto px-0 mb-2">
        Voltar para o painel
      </Link>

      <TituloDeSecao descricao="O que você vende. Cada produto aqui aparece na busca pública.">
        Meus produtos
      </TituloDeSecao>

      {erro && (
        <Aviso tipo="erro" className="mb-5">
          {erro}
        </Aviso>
      )}

      {emEdicao ? (
        <FormularioDeProduto
          produto={emEdicao === 'novo' ? null : emEdicao}
          categorias={categorias}
          aoTerminar={() => {
            definirEmEdicao(null);
            recarregar();
          }}
          aoCancelar={() => definirEmEdicao(null)}
        />
      ) : (
        <Botao className="mb-6" onClick={() => definirEmEdicao('novo')}>
          Cadastrar um produto
        </Botao>
      )}

      {carregando && <Carregando />}

      {!carregando && produtos.length === 0 && !emEdicao && (
        <Aviso tipo="atencao" titulo="Seu catálogo está vazio">
          Cadastre pelo menos um produto para as pessoas encontrarem você na busca. Pode ser um
          serviço também, com preço "sob consulta".
        </Aviso>
      )}

      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {produtos.map((produto) => (
          <article key={produto.id} className="quadro carimbo-leve overflow-hidden flex flex-col">
            <Foto url={produto.imagemUrl} nome={produto.nome} arco={false} />

            <div className="p-3 flex flex-col gap-2 grow">
              <h3 className="text-lg leading-snug">{produto.nome}</h3>
              <p className="preco self-start">{produto.precoFormatado}</p>

              {!produto.disponivel && (
                <p className="text-sm font-semibold text-alerta">
                  Marcado como esgotado: não aparece na busca.
                </p>
              )}

              <div className="mt-auto pt-2 flex flex-wrap gap-2">
                <Botao variante="neutro" onClick={() => definirEmEdicao(produto)}>
                  Alterar
                </Botao>
                <Botao variante="neutro" onClick={() => alternarDisponibilidade(produto)}>
                  {produto.disponivel ? 'Marcar esgotado' : 'Voltou a ter'}
                </Botao>
                <Botao variante="texto" onClick={() => retirar(produto)}>
                  Retirar
                </Botao>
              </div>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}

function FormularioDeProduto({
  produto,
  categorias,
  aoTerminar,
  aoCancelar,
}: {
  produto: ProdutoDoPainel | null;
  categorias: CategoriaDoCatalogo[];
  aoTerminar(): void;
  aoCancelar(): void;
}) {
  const categoriaAtual = categorias.find((categoria) => categoria.id === produto?.categoriaId);

  const [campos, definirCampos] = useState({
    nome: produto?.nome ?? '',
    descricao: produto?.descricao ?? '',
    preco: centavosParaCampo(produto?.precoEmCentavos),
    categoria: categoriaAtual?.nome ?? '',
  });
  const [arquivo, definirArquivo] = useState<File | null>(null);
  const [erros, definirErros] = useState<Record<string, string>>({});
  const [erroGeral, definirErroGeral] = useState<string | null>(null);
  const [salvando, definirSalvando] = useState(false);

  const salvar = async (evento: React.FormEvent) => {
    evento.preventDefault();
    definirErros({});
    definirErroGeral(null);
    definirSalvando(true);

    try {
      const corpo = {
        nome: campos.nome,
        descricao: campos.descricao,
        precoEmCentavos: campos.preco.trim() === '' ? null : paraCentavos(campos.preco),
        categoria: campos.categoria,
      };

      const salvo = produto
        ? await chamar<ProdutoDoPainel>(`/api/catalogo/meus-produtos/${produto.id}`, {
            metodo: 'PUT',
            corpo,
          })
        : await chamar<ProdutoDoPainel>('/api/catalogo/meus-produtos', {
            metodo: 'POST',
            corpo,
          });

      if (arquivo) {
        const formulario = new FormData();
        formulario.append('arquivo', arquivo);
        try {
          await chamar(`/api/catalogo/meus-produtos/${salvo.id}/imagem`, {
            metodo: 'POST',
            arquivo: formulario,
          });
        } catch (falhaDaFoto) {
          // O produto já foi salvo. A foto falhou sozinha, e é isso que a
          // mensagem precisa dizer, senão a pessoa cadastra tudo de novo.
          definirErroGeral(
            falhaDaFoto instanceof ErroDaApi
              ? `O produto foi salvo, mas a foto não subiu: ${falhaDaFoto.message}`
              : 'O produto foi salvo, mas a foto não subiu.',
          );
          definirSalvando(false);
          return;
        }
      }

      aoTerminar();
    } catch (falha) {
      if (falha instanceof ErroDaApi) {
        definirErros(falha.campos);
        definirErroGeral(Object.keys(falha.campos).length === 0 ? falha.message : null);
      } else {
        definirErroGeral('Não foi possível salvar agora.');
      }
      definirSalvando(false);
    }
  };

  return (
    <form className="quadro p-5 flex flex-col gap-4 mb-8" onSubmit={salvar} noValidate>
      <h3 className="font-display text-xl font-bold">
        {produto ? 'Alterar produto' : 'Novo produto'}
      </h3>

      {erroGeral && <Aviso tipo="erro">{erroGeral}</Aviso>}

      <Campo
        etiqueta="Nome do produto ou serviço"
        value={campos.nome}
        erro={erros.nome}
        onChange={(evento) => definirCampos((atual) => ({ ...atual, nome: evento.target.value }))}
        required
      />

      <AreaDeTexto
        etiqueta="Descrição"
        ajuda="Tamanho, sabor, prazo de entrega, o que estiver incluso."
        rows={3}
        maxLength={800}
        value={campos.descricao}
        erro={erros.descricao}
        onChange={(evento) =>
          definirCampos((atual) => ({ ...atual, descricao: evento.target.value }))
        }
      />

      <div className="grid gap-4 sm:grid-cols-2">
        <Campo
          etiqueta="Preço"
          ajuda='Deixe em branco para aparecer como "sob consulta".'
          value={campos.preco}
          erro={erros.precoEmCentavos}
          inputMode="decimal"
          placeholder="12,50"
          onChange={(evento) =>
            definirCampos((atual) => ({ ...atual, preco: evento.target.value }))
          }
        />

        <Selecao
          etiqueta="Categoria"
          vazio="Escolha uma categoria"
          opcoes={categorias.map((categoria) => categoria.nome)}
          value={campos.categoria}
          erro={erros.categoria}
          onChange={(evento) =>
            definirCampos((atual) => ({ ...atual, categoria: evento.target.value }))
          }
          required
        />
      </div>

      <div>
        <label className="etiqueta" htmlFor="foto">
          Foto do produto
        </label>
        <p className="text-sm text-concreto mb-1">
          JPG, PNG ou WEBP, até 5 MB. A foto é o que mais faz diferença na busca.
        </p>
        <input
          id="foto"
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          className="campo"
          onChange={(evento) => definirArquivo(evento.target.files?.[0] ?? null)}
        />
      </div>

      <div className="flex gap-3">
        <Botao type="submit" carregando={salvando}>
          {produto ? 'Salvar alterações' : 'Publicar produto'}
        </Botao>
        <Botao type="button" variante="neutro" onClick={aoCancelar}>
          Cancelar
        </Botao>
      </div>
    </form>
  );
}
