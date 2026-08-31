import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Vitrine } from './Vitrine';
import {
  fetchDeMentira,
  produtoDeExemplo,
  renderizar,
  resposta,
  resumoDeExemplo,
} from '@/testes/ajudantes';

function paginaCom(produtos: unknown[], total = produtos.length) {
  return {
    conteudo: produtos,
    pagina: 0,
    tamanho: 24,
    total,
    totalDePaginas: Math.max(1, Math.ceil(total / 24)),
    temProxima: total > 24,
  };
}

describe('vitrine pública', () => {
  it('mostra os produtos assim que abre, sem exigir busca', async () => {
    vi.stubGlobal(
      'fetch',
      fetchDeMentira({
        '/api/busca/resumo': resumoDeExemplo,
        '/api/busca/produtos': paginaCom([produtoDeExemplo]),
      }),
    );

    renderizar(<Vitrine />);

    expect(await screen.findByRole('heading', { name: 'Bolo de pote' })).toBeInTheDocument();
    expect(screen.getByText('R$ 12,00')).toBeInTheDocument();
  });

  it('mostra quantas lojas e produtos existem na vitrine', async () => {
    vi.stubGlobal(
      'fetch',
      fetchDeMentira({
        '/api/busca/resumo': resumoDeExemplo,
        '/api/busca/produtos': paginaCom([produtoDeExemplo]),
      }),
    );

    renderizar(<Vitrine />);

    expect(await screen.findByText('12')).toBeInTheDocument();
    expect(screen.getByText('38')).toBeInTheDocument();
  });

  it('procura pelo termo digitado', async () => {
    const espiao = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/busca/resumo')) {
        return Promise.resolve(resposta(resumoDeExemplo));
      }
      return Promise.resolve(resposta(paginaCom([produtoDeExemplo])));
    });
    vi.stubGlobal('fetch', espiao);

    renderizar(<Vitrine />);

    await userEvent.type(await screen.findByLabelText('O que você procura'), 'bolo');
    await userEvent.click(screen.getByRole('button', { name: 'Procurar' }));

    await waitFor(() => {
      const busca = espiao.mock.calls
        .map((chamada) => String(chamada[0]))
        .find((url) => url.includes('termo=bolo'));
      expect(busca).toBeDefined();
    });
  });

  it('oferece os bairros num seletor e as categorias como atalho', async () => {
    vi.stubGlobal(
      'fetch',
      fetchDeMentira({
        '/api/busca/resumo': resumoDeExemplo,
        '/api/busca/produtos': paginaCom([produtoDeExemplo]),
      }),
    );

    renderizar(<Vitrine />);

    // Bairro é seletor porque Bauru tem dezenas deles; categoria é fila de
    // atalhos porque são doze e cabem numa linha.
    expect(await screen.findByRole('option', { name: 'Vila Cardia' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Todos os bairros' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Alimentação' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Tudo' })).toBeInTheDocument();
  });

  it('filtra pela categoria ao clicar no atalho', async () => {
    const espiao = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/busca/resumo')) {
        return Promise.resolve(resposta(resumoDeExemplo));
      }
      return Promise.resolve(resposta(paginaCom([produtoDeExemplo])));
    });
    vi.stubGlobal('fetch', espiao);

    renderizar(<Vitrine />);

    await userEvent.click(await screen.findByRole('button', { name: 'Pet' }));

    await waitFor(() => {
      const busca = espiao.mock.calls
        .map((chamada) => String(chamada[0]))
        .find((url) => url.includes('categoria=Pet'));
      expect(busca).toBeDefined();
    });
  });

  it('explica como a plataforma funciona no fim da página', async () => {
    vi.stubGlobal(
      'fetch',
      fetchDeMentira({
        '/api/busca/resumo': resumoDeExemplo,
        '/api/busca/produtos': paginaCom([produtoDeExemplo]),
      }),
    );

    renderizar(<Vitrine />);

    expect(await screen.findByRole('heading', { name: 'Como funciona' })).toBeInTheDocument();
    expect(screen.getByText('Você encontra')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Cadastrar meu negócio/ })).toBeInTheDocument();
  });

  it('filtra por bairro pela URL, para o link poder ser compartilhado', async () => {
    const espiao = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/busca/resumo')) {
        return Promise.resolve(resposta(resumoDeExemplo));
      }
      return Promise.resolve(resposta(paginaCom([produtoDeExemplo])));
    });
    vi.stubGlobal('fetch', espiao);

    renderizar(<Vitrine />, '/?bairro=Vila+Cardia');

    await waitFor(() => {
      const busca = espiao.mock.calls
        .map((chamada) => String(chamada[0]))
        .find((url) => url.includes('bairro=Vila'));
      expect(busca).toBeDefined();
    });
  });

  it('explica o vazio em vez de mostrar uma tela em branco', async () => {
    vi.stubGlobal(
      'fetch',
      fetchDeMentira({
        '/api/busca/resumo': resumoDeExemplo,
        '/api/busca/produtos': paginaCom([]),
      }),
    );

    renderizar(<Vitrine />);

    expect(await screen.findByText(/Nada encontrado com esses filtros/)).toBeInTheDocument();
    expect(screen.getByText(/Casa do Empreendedor/)).toBeInTheDocument();
  });

  it('mostra o erro quando a busca falha, com caminho de saída', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url: string) => {
        if (url.includes('/api/busca/resumo')) {
          return Promise.resolve(resposta(resumoDeExemplo));
        }
        return Promise.resolve(resposta({ detail: 'Serviço indisponível' }, 503));
      }),
    );

    renderizar(<Vitrine />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Serviço indisponível');
  });

  it('conta os resultados encontrados', async () => {
    vi.stubGlobal(
      'fetch',
      fetchDeMentira({
        '/api/busca/resumo': resumoDeExemplo,
        '/api/busca/produtos': paginaCom([produtoDeExemplo, { ...produtoDeExemplo, id: 'p2' }], 2),
      }),
    );

    renderizar(<Vitrine />);

    expect(await screen.findByText('2 resultados')).toBeInTheDocument();
  });

  it('mostra a paginação só quando há mais de uma página', async () => {
    vi.stubGlobal(
      'fetch',
      fetchDeMentira({
        '/api/busca/resumo': resumoDeExemplo,
        '/api/busca/produtos': paginaCom([produtoDeExemplo], 100),
      }),
    );

    renderizar(<Vitrine />);

    expect(await screen.findByRole('navigation', { name: 'Paginação' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled();
  });

  it('o campo de busca é uma região de busca de verdade, para o leitor de tela', async () => {
    vi.stubGlobal(
      'fetch',
      fetchDeMentira({
        '/api/busca/resumo': resumoDeExemplo,
        '/api/busca/produtos': paginaCom([produtoDeExemplo]),
      }),
    );

    renderizar(<Vitrine />);

    expect(await screen.findByRole('search')).toBeInTheDocument();
  });
});
