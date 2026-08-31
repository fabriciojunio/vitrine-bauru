import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { chamar } from '@/lib/api';
import type { LojaNaVitrine, Pagina, ResumoDaVitrine } from '@/lib/tipos';
import { Aviso, Botao, Carregando, Selecao, TituloDeSecao } from '@/componentes/Basicos';
import { CartaoDeLoja } from '@/componentes/Cartoes';

/**
 * A lista de lojas.
 *
 * Existe separada da busca por produto porque as duas perguntas são
 * diferentes: "quero um bolo de pote" e "quem vende doce na Vila Cardia" não
 * se respondem com a mesma tela. A segunda é a que a SEDECON usa quando
 * alguém liga perguntando por um serviço.
 */
export function Lojas() {
  const [parametros, definirParametros] = useSearchParams();
  const [resultado, definirResultado] = useState<Pagina<LojaNaVitrine> | null>(null);
  const [resumo, definirResumo] = useState<ResumoDaVitrine | null>(null);
  const [carregando, definirCarregando] = useState(true);
  const [erro, definirErro] = useState<string | null>(null);

  const bairro = parametros.get('bairro') ?? '';
  const categoria = parametros.get('categoria') ?? '';

  useEffect(() => {
    chamar<ResumoDaVitrine>('/api/busca/resumo')
      .then(definirResumo)
      .catch(() => definirResumo(null));
  }, []);

  useEffect(() => {
    definirCarregando(true);
    definirErro(null);

    const consulta = new URLSearchParams();
    if (bairro) consulta.set('bairro', bairro);
    if (categoria) consulta.set('categoria', categoria);
    consulta.set('tamanho', '24');

    chamar<Pagina<LojaNaVitrine>>(`/api/busca/lojas?${consulta}`)
      .then(definirResultado)
      .catch((falha) => definirErro(falha.message))
      .finally(() => definirCarregando(false));
  }, [bairro, categoria]);

  const trocarFiltro = (chave: string, valor: string) => {
    const novos = new URLSearchParams(parametros);
    if (valor) {
      novos.set(chave, valor);
    } else {
      novos.delete(chave);
    }
    definirParametros(novos);
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <TituloDeSecao descricao="Quem está na plataforma, por bairro e por ramo de atividade.">
        Lojas de Bauru
      </TituloDeSecao>

      <div className="grid gap-3 sm:grid-cols-3 items-end mb-8">
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
        {(bairro || categoria) && (
          <Botao variante="neutro" onClick={() => definirParametros(new URLSearchParams())}>
            Limpar filtros
          </Botao>
        )}
      </div>

      {erro && (
        <Aviso tipo="erro" titulo="Não deu para carregar as lojas">
          {erro}
        </Aviso>
      )}

      {carregando && <Carregando />}

      {!carregando && resultado?.conteudo.length === 0 && (
        <Aviso tipo="atencao" titulo="Nenhuma loja com esses filtros">
          Tente outro bairro ou veja todas as categorias.
        </Aviso>
      )}

      {!carregando && resultado && resultado.conteudo.length > 0 && (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {resultado.conteudo.map((loja) => (
            <CartaoDeLoja key={loja.id} loja={loja} />
          ))}
        </div>
      )}
    </div>
  );
}
