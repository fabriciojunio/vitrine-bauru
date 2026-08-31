import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Moderacao } from './Moderacao';
import { renderizar, resposta } from '@/testes/ajudantes';

const cadastro = {
  id: 'loja-1',
  nomeDoNegocio: 'Doces da Lourdes',
  descricao: 'Bolo de pote e salgado de festa',
  categoriaPrincipal: 'Alimentação',
  bairro: 'Vila Cardia',
  telefoneWhatsapp: '(14) 99712-3456',
  documento: '***.982.247-**',
  tipoDoDocumento: 'CPF',
  situacaoDoDocumento: null,
  situacao: 'PENDENTE',
  cadastradoEm: new Date(Date.now() - 9 * 86_400_000).toISOString(),
  diasNaFila: 9,
};

function filaCom(itens: unknown[]) {
  return {
    conteudo: itens,
    pagina: 0,
    tamanho: 30,
    total: itens.length,
    totalDePaginas: 1,
    temProxima: false,
  };
}

describe('fila de moderação da SEDECON', () => {
  it('lista quem está esperando análise', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta(filaCom([cadastro]))));

    renderizar(<Moderacao />);

    expect(await screen.findByRole('heading', { name: 'Doces da Lourdes' })).toBeInTheDocument();
    expect(screen.getByText('Alimentação · Vila Cardia')).toBeInTheDocument();
  });

  it('mostra há quanto tempo o cadastro espera, que é o que ordena a fila', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta(filaCom([cadastro]))));

    renderizar(<Moderacao />);

    expect(await screen.findByText(/esperando há 9 dias/)).toBeInTheDocument();
  });

  it('mostra o documento mascarado para quem modera', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta(filaCom([cadastro]))));

    renderizar(<Moderacao />);

    expect(await screen.findByText(/\*\*\*\.982\.247-\*\*/)).toBeInTheDocument();
  });

  it('avisa quando a consulta à Receita ainda não terminou', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta(filaCom([cadastro]))));

    renderizar(<Moderacao />);

    expect(await screen.findByText('Consulta ainda não concluída')).toBeInTheDocument();
  });

  it('aprova o cadastro', async () => {
    const espiao = vi.fn().mockResolvedValue(resposta(filaCom([cadastro])));
    vi.stubGlobal('fetch', espiao);

    renderizar(<Moderacao />);

    await userEvent.click(await screen.findByRole('button', { name: /Aprovar e colocar no ar/ }));

    await waitFor(() => {
      const aprovacao = espiao.mock.calls.find((chamada) =>
        String(chamada[0]).includes('/loja-1/aprovar'),
      );
      expect(aprovacao).toBeDefined();
      expect(aprovacao![1]?.method).toBe('POST');
    });
  });

  it('não deixa recusar sem escrever o motivo', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta(filaCom([cadastro]))));

    renderizar(<Moderacao />);

    await userEvent.click(await screen.findByRole('button', { name: /Recusar com motivo/ }));

    expect(screen.getByRole('button', { name: /Confirmar recusa/ })).toBeDisabled();
  });

  it('libera a recusa quando o motivo é escrito', async () => {
    const espiao = vi.fn().mockResolvedValue(resposta(filaCom([cadastro])));
    vi.stubGlobal('fetch', espiao);

    renderizar(<Moderacao />);

    await userEvent.click(await screen.findByRole('button', { name: /Recusar com motivo/ }));
    await userEvent.type(
      screen.getByLabelText(/Motivo da recusa/),
      'O documento não confere com o nome do negócio.',
    );

    const confirmar = screen.getByRole('button', { name: /Confirmar recusa/ });
    expect(confirmar).toBeEnabled();

    await userEvent.click(confirmar);

    await waitFor(() => {
      const recusa = espiao.mock.calls.find((chamada) =>
        String(chamada[0]).includes('/loja-1/rejeitar'),
      );
      expect(recusa).toBeDefined();
      expect(String(recusa![1].body)).toContain('não confere');
    });
  });

  it('avisa que o motivo vai no e-mail do empreendedor', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta(filaCom([cadastro]))));

    renderizar(<Moderacao />);

    await userEvent.click(await screen.findByRole('button', { name: /Recusar com motivo/ }));

    expect(screen.getByText(/vai no e-mail do empreendedor/)).toBeInTheDocument();
  });

  it('comemora a fila vazia em vez de mostrar tela em branco', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta(filaCom([]))));

    renderizar(<Moderacao />);

    expect(await screen.findByText('Nenhum cadastro esperando')).toBeInTheDocument();
  });

  it('mostra o erro quando a fila não carrega', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(resposta({ detail: 'Sua conta não tem permissão' }, 403)),
    );

    renderizar(<Moderacao />);

    expect(await screen.findByRole('alert')).toHaveTextContent('não tem permissão');
  });
});
