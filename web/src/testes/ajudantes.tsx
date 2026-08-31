import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { ProvedorDaSessao } from '@/lib/sessao';

/**
 * Montagem de tela para os testes.
 *
 * Quase todo componente aqui depende de duas coisas do ambiente: o roteador,
 * porque a navegação faz parte do fluxo, e o contexto de sessão, porque o
 * cabeçalho muda conforme quem está logado. Repetir isso em cada teste
 * esconderia o que cada um está de fato verificando.
 */
export function renderizar(elemento: ReactElement, rotaInicial = '/') {
  return render(
    <MemoryRouter initialEntries={[rotaInicial]}>
      <ProvedorDaSessao>{elemento}</ProvedorDaSessao>
    </MemoryRouter>,
  );
}

/** Sem o provedor de sessão: para componentes que não dependem de login. */
export function renderizarSemSessao(elemento: ReactElement, rotaInicial = '/') {
  return render(<MemoryRouter initialEntries={[rotaInicial]}>{elemento}</MemoryRouter>);
}

export function resposta(corpo: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers({ 'Content-Type': 'application/json' }),
    json: async () => corpo,
    text: async () => JSON.stringify(corpo),
  } as Response;
}

/**
 * Encaminha cada endereço para a resposta combinada.
 *
 * Deixa o teste declarar só o que interessa a ele: o resto responde vazio, em
 * vez de estourar erro de rede e poluir a saída com falha que não é do teste.
 */
export function fetchDeMentira(rotas: Record<string, unknown>) {
  return vi.fn().mockImplementation((url: string) => {
    const encontrada = Object.keys(rotas).find((caminho) => String(url).includes(caminho));
    if (encontrada) {
      const valor = rotas[encontrada];
      if (valor instanceof Error) {
        return Promise.reject(valor);
      }
      return Promise.resolve(resposta(valor));
    }
    return Promise.resolve(resposta({}));
  });
}

export const produtoDeExemplo = {
  id: 'produto-1',
  nome: 'Bolo de pote',
  descricao: 'Massa de chocolate com brigadeiro',
  precoEmCentavos: 1200,
  precoFormatado: 'R$ 12,00',
  categoria: 'Alimentação',
  imagemUrl: null,
  empreendedorId: 'loja-1',
  lojaNome: 'Doces da Lourdes',
  lojaApelido: 'doces-da-lourdes',
  bairro: 'Vila Cardia',
};

export const lojaDeExemplo = {
  id: 'loja-1',
  nomeDoNegocio: 'Doces da Lourdes',
  apelidoNaUrl: 'doces-da-lourdes',
  descricao: 'Bolo de pote e salgado de festa por encomenda',
  categoria: 'Alimentação',
  bairro: 'Vila Cardia',
  telefoneWhatsapp: '(14) 99712-3456',
  fotoDeCapaUrl: null,
};

export const resumoDeExemplo = {
  lojas: 12,
  produtos: 38,
  bairros: ['Centro', 'Vila Cardia', 'Vila Falcão'],
  categorias: ['Alimentação', 'Artesanato', 'Pet'],
};
