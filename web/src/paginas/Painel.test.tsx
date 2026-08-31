import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Painel } from './Painel';
import { renderizar, resposta } from '@/testes/ajudantes';
import type { MinhaLoja } from '@/lib/tipos';

const lojaBase: MinhaLoja = {
  id: 'loja-1',
  nomeDoNegocio: 'Doces da Lourdes',
  apelidoNaUrl: 'doces-da-lourdes',
  descricao: 'Bolo de pote e salgado de festa',
  categoriaPrincipal: 'Alimentação',
  bairro: 'Vila Cardia',
  cep: '17011066',
  telefoneWhatsapp: '(14) 99712-3456',
  documento: '***.982.247-**',
  fotoDeCapaUrl: null,
  situacao: 'APROVADO',
  motivoDaModeracao: null,
  cadastradoEm: '2026-09-01T10:00:00Z',
  moderadoEm: '2026-09-03T10:00:00Z',
  apareceNaVitrine: true,
};

function fetchCom(loja: Partial<MinhaLoja>, indicadores = { produtos: 4, contatosNoTotal: 12, contatosNosUltimos30Dias: 5 }) {
  return vi.fn().mockImplementation((url: string) => {
    if (url.includes('/minha-loja/indicadores')) {
      return Promise.resolve(resposta(indicadores));
    }
    if (url.includes('/minha-loja')) {
      return Promise.resolve(resposta({ ...lojaBase, ...loja }));
    }
    return Promise.resolve(resposta([]));
  });
}

describe('painel do empreendedor', () => {
  it('a primeira informação da tela é a situação do cadastro', async () => {
    vi.stubGlobal('fetch', fetchCom({}));

    renderizar(<Painel />);

    expect(await screen.findByText('Sua loja está no ar')).toBeInTheDocument();
  });

  it('mostra os números da loja', async () => {
    vi.stubGlobal('fetch', fetchCom({}));

    renderizar(<Painel />);

    expect(await screen.findByText('4')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText('Contatos recebidos')).toBeInTheDocument();
  });

  it('explica a espera quando o cadastro está pendente, e sugere o que fazer', async () => {
    vi.stubGlobal('fetch', fetchCom({ situacao: 'PENDENTE', apareceNaVitrine: false }));

    renderizar(<Painel />);

    expect(await screen.findByText(/está na fila da SEDECON/)).toBeInTheDocument();
    expect(screen.getByText(/Aproveite para cadastrar seus produtos/)).toBeInTheDocument();
  });

  it('mostra o motivo da recusa e o caminho para corrigir', async () => {
    vi.stubGlobal(
      'fetch',
      fetchCom({
        situacao: 'REJEITADO',
        apareceNaVitrine: false,
        motivoDaModeracao: 'A descrição não explica o que você vende.',
      }),
    );

    renderizar(<Painel />);

    expect(await screen.findByText('A descrição não explica o que você vende.')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /Já corrigi, enviar de novo/ }),
    ).toBeInTheDocument();
  });

  it('reenvia o cadastro corrigido para a análise', async () => {
    const espiao = fetchCom({
      situacao: 'REJEITADO',
      apareceNaVitrine: false,
      motivoDaModeracao: 'Falta detalhar os produtos.',
    });
    vi.stubGlobal('fetch', espiao);

    renderizar(<Painel />);

    await userEvent.click(await screen.findByRole('button', { name: /Já corrigi/ }));

    const reenvio = espiao.mock.calls.find((chamada) =>
      String(chamada[0]).includes('/minha-loja/reenviar'),
    );
    expect(reenvio).toBeDefined();
  });

  it('avisa a suspensão e tranquiliza sobre os produtos', async () => {
    vi.stubGlobal(
      'fetch',
      fetchCom({
        situacao: 'SUSPENSO',
        apareceNaVitrine: false,
        motivoDaModeracao: 'Denúncia em análise.',
      }),
    );

    renderizar(<Painel />);

    expect(await screen.findByText('Sua loja está suspensa')).toBeInTheDocument();
    expect(screen.getByText(/produtos continuam salvos/)).toBeInTheDocument();
  });

  it('só oferece o link público quando a loja está de fato no ar', async () => {
    vi.stubGlobal('fetch', fetchCom({ situacao: 'PENDENTE', apareceNaVitrine: false }));

    renderizar(<Painel />);

    await screen.findByText(/está na fila da SEDECON/);
    expect(screen.queryByRole('link', { name: /Ver minha loja como o cliente vê/ })).toBeNull();
  });

  it('mostra o documento mascarado, e nunca inteiro', async () => {
    vi.stubGlobal('fetch', fetchCom({}));

    renderizar(<Painel />);

    expect(await screen.findByText('***.982.247-**')).toBeInTheDocument();
  });

  it('abre o formulário de alteração dos dados', async () => {
    vi.stubGlobal('fetch', fetchCom({}));

    renderizar(<Painel />);

    await userEvent.click(await screen.findByRole('button', { name: 'Alterar meus dados' }));

    expect(screen.getByLabelText('Nome do negócio')).toHaveValue('Doces da Lourdes');
  });

  it('dá o recado de boas-vindas para quem acabou de se cadastrar', async () => {
    vi.stubGlobal('fetch', fetchCom({ situacao: 'PENDENTE', apareceNaVitrine: false }));

    renderizar(<Painel />, '/painel?novo=1');

    expect(await screen.findByText('Cadastro enviado')).toBeInTheDocument();
  });

  it('mostra um erro compreensível quando o painel não carrega', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(resposta({ detail: 'Sua conta não tem uma loja vinculada.' }, 404)),
    );

    renderizar(<Painel />);

    expect(await screen.findByRole('alert')).toHaveTextContent('não tem uma loja vinculada');
  });
});
