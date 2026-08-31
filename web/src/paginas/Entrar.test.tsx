import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Entrar } from './Entrar';
import { fetchDeMentira, renderizar, resposta } from '@/testes/ajudantes';

const sessaoDaEmpreendedora = {
  tokenDeAcesso: 'acesso',
  tokenDeRenovacao: 'renovacao',
  expiraEm: '2026-09-22T13:00:00Z',
  usuario: {
    id: 'usuario-1',
    nome: 'Maria de Lourdes',
    email: 'lourdes@exemplo.com',
    papel: 'EMPREENDEDOR',
    empreendedorId: 'loja-1',
  },
};

describe('tela de entrar', () => {
  it('mostra o formulário de e-mail e senha', async () => {
    vi.stubGlobal('fetch', fetchDeMentira({ '/auth/demonstracao': { ativa: false } }));

    renderizar(<Entrar />);

    expect(await screen.findByLabelText('E-mail')).toBeInTheDocument();
    expect(screen.getByLabelText('Senha')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeInTheDocument();
  });

  it('envia e-mail e senha para a API', async () => {
    const espiao = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/auth/demonstracao')) {
        return Promise.resolve(resposta({ ativa: false }));
      }
      return Promise.resolve(resposta(sessaoDaEmpreendedora));
    });
    vi.stubGlobal('fetch', espiao);

    renderizar(<Entrar />);

    await userEvent.type(await screen.findByLabelText('E-mail'), 'lourdes@exemplo.com');
    await userEvent.type(screen.getByLabelText('Senha'), 'bolodefuba2026');
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => {
      const chamadaDeLogin = espiao.mock.calls.find((chamada) =>
        String(chamada[0]).includes('/auth/login'),
      );
      expect(chamadaDeLogin).toBeDefined();
      expect(String(chamadaDeLogin![1].body)).toContain('lourdes@exemplo.com');
    });
  });

  it('mostra a mensagem do servidor quando a senha está errada', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url: string) => {
        if (url.includes('/auth/demonstracao')) {
          return Promise.resolve(resposta({ ativa: false }));
        }
        return Promise.resolve(resposta({ detail: 'E-mail ou senha incorretos.' }, 401));
      }),
    );

    renderizar(<Entrar />);

    await userEvent.type(await screen.findByLabelText('E-mail'), 'lourdes@exemplo.com');
    await userEvent.type(screen.getByLabelText('Senha'), 'errada12345');
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha incorretos.');
  });

  it('leva para o cadastro quem ainda não tem conta', async () => {
    vi.stubGlobal('fetch', fetchDeMentira({ '/auth/demonstracao': { ativa: false } }));

    renderizar(<Entrar />);

    expect(await screen.findByRole('link', { name: /Cadastre seu negócio/ })).toHaveAttribute(
      'href',
      '/cadastrar',
    );
  });
});

describe('modo demonstração na tela de entrar', () => {
  it('não mostra os botões de demonstração quando o ambiente é de produção', async () => {
    vi.stubGlobal('fetch', fetchDeMentira({ '/auth/demonstracao': { ativa: false } }));

    renderizar(<Entrar />);

    await screen.findByLabelText('E-mail');
    expect(screen.queryByRole('button', { name: /Entrar como empreendedora/ })).toBeNull();
    expect(screen.queryByRole('button', { name: /Entrar como SEDECON/ })).toBeNull();
    expect(screen.getByText(/Primeira vez por aqui/)).toBeInTheDocument();
  });

  it('mostra os dois botões quando a demonstração está ligada', async () => {
    vi.stubGlobal('fetch', fetchDeMentira({ '/auth/demonstracao': { ativa: true } }));

    renderizar(<Entrar />);

    expect(
      await screen.findByRole('button', { name: /Entrar como empreendedora/ }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Entrar como SEDECON/ })).toBeInTheDocument();
  });

  it('avisa que os dados da demonstração são fictícios', async () => {
    vi.stubGlobal('fetch', fetchDeMentira({ '/auth/demonstracao': { ativa: true } }));

    renderizar(<Entrar />);

    expect(await screen.findByText(/lojas fictícias/)).toBeInTheDocument();
    expect(screen.getByText(/Nada aqui é dado real/)).toBeInTheDocument();
  });

  it('entra em um clique como empreendedora', async () => {
    const espiao = vi.fn().mockImplementation((url: string, opcoes?: RequestInit) => {
      if (url.includes('/auth/demonstracao') && opcoes?.method === 'POST') {
        return Promise.resolve(resposta(sessaoDaEmpreendedora));
      }
      if (url.includes('/auth/demonstracao')) {
        return Promise.resolve(resposta({ ativa: true }));
      }
      return Promise.resolve(resposta({}));
    });
    vi.stubGlobal('fetch', espiao);

    renderizar(<Entrar />);

    await userEvent.click(
      await screen.findByRole('button', { name: /Entrar como empreendedora/ }),
    );

    await waitFor(() => {
      const chamada = espiao.mock.calls.find(
        (item) => String(item[0]).includes('/auth/demonstracao') && item[1]?.method === 'POST',
      );
      expect(String(chamada![1].body)).toContain('empreendedor');
    });
  });

  it('entra em um clique como SEDECON', async () => {
    const espiao = vi.fn().mockImplementation((url: string, opcoes?: RequestInit) => {
      if (url.includes('/auth/demonstracao') && opcoes?.method === 'POST') {
        return Promise.resolve(
          resposta({
            ...sessaoDaEmpreendedora,
            usuario: { ...sessaoDaEmpreendedora.usuario, papel: 'ADMIN_SEDECON' },
          }),
        );
      }
      if (url.includes('/auth/demonstracao')) {
        return Promise.resolve(resposta({ ativa: true }));
      }
      return Promise.resolve(resposta({}));
    });
    vi.stubGlobal('fetch', espiao);

    renderizar(<Entrar />);

    await userEvent.click(await screen.findByRole('button', { name: /Entrar como SEDECON/ }));

    await waitFor(() => {
      const chamada = espiao.mock.calls.find(
        (item) => String(item[0]).includes('/auth/demonstracao') && item[1]?.method === 'POST',
      );
      expect(String(chamada![1].body)).toContain('sedecon');
    });
  });

  it('explica o que cada botão mostra, para quem nunca viu o sistema', async () => {
    vi.stubGlobal('fetch', fetchDeMentira({ '/auth/demonstracao': { ativa: true } }));

    renderizar(<Entrar />);

    expect(await screen.findByText(/Doces da Lourdes/)).toBeInTheDocument();
    expect(screen.getByText(/fila de moderação/)).toBeInTheDocument();
  });

  it('some com os botões quando o servidor não responde sobre a demonstração', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('sem rede')));

    renderizar(<Entrar />);

    await screen.findByLabelText('E-mail');
    expect(screen.queryByRole('button', { name: /Entrar como SEDECON/ })).toBeNull();
  });
});
