import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Cadastrar } from './Cadastrar';
import { renderizar, resposta } from '@/testes/ajudantes';

const BAIRROS = ['Centro', 'Vila Cardia', 'Vila Falcão'];
const CATEGORIAS = ['Alimentação', 'Artesanato', 'Pet'];

function respostasPadrao(aoCadastrar?: () => Promise<Response>) {
  return vi.fn().mockImplementation((url: string, opcoes?: RequestInit) => {
    if (url.includes('/api/cadastro/bairros')) {
      return Promise.resolve(resposta(BAIRROS));
    }
    if (url.includes('/api/cadastro/categorias')) {
      return Promise.resolve(resposta(CATEGORIAS));
    }
    if (url.includes('/api/cadastro/empreendedores') && opcoes?.method === 'POST') {
      return aoCadastrar
        ? aoCadastrar()
        : Promise.resolve(resposta({ empreendedorId: 'loja-1', apelidoNaUrl: 'doces' }, 201));
    }
    if (url.includes('viacep')) {
      return Promise.resolve(resposta({ bairro: 'Vila Cardia', localidade: 'Bauru' }));
    }
    return Promise.resolve(resposta({ tokenDeAcesso: 'a', tokenDeRenovacao: 'r', usuario: {} }));
  });
}

describe('formulário de cadastro', () => {
  it('carrega os bairros e as categorias que o servidor aceita', async () => {
    vi.stubGlobal('fetch', respostasPadrao());

    renderizar(<Cadastrar />);

    expect(await screen.findByRole('option', { name: 'Vila Cardia' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Alimentação' })).toBeInTheDocument();
  });

  it('explica cada campo em vez de só rotular', async () => {
    vi.stubGlobal('fetch', respostasPadrao());

    renderizar(<Cadastrar />);

    expect(
      await screen.findByText(/é por aqui que a SEDECON avisa/i),
    ).toBeInTheDocument();
    expect(screen.getByText(/Se você é MEI, use o CNPJ/)).toBeInTheDocument();
    expect(screen.getByText(/É por aqui que o cliente vai falar com você/)).toBeInTheDocument();
  });

  it('mascara o telefone enquanto a pessoa digita', async () => {
    vi.stubGlobal('fetch', respostasPadrao());

    renderizar(<Cadastrar />);

    const campo = await screen.findByLabelText('Celular com WhatsApp');
    await userEvent.type(campo, '14997123456');

    expect(campo).toHaveValue('(14) 99712-3456');
  });

  it('mascara o CPF enquanto a pessoa digita', async () => {
    vi.stubGlobal('fetch', respostasPadrao());

    renderizar(<Cadastrar />);

    const campo = await screen.findByLabelText('CPF ou CNPJ');
    await userEvent.type(campo, '52998224725');

    expect(campo).toHaveValue('529.982.247-25');
  });

  it('preenche o bairro sozinho a partir do CEP', async () => {
    vi.stubGlobal('fetch', respostasPadrao());

    renderizar(<Cadastrar />);

    const cep = await screen.findByLabelText('CEP');
    await userEvent.type(cep, '17011066');
    await userEvent.tab();

    await waitFor(() => {
      expect(screen.getByLabelText('Bairro')).toHaveValue('Vila Cardia');
    });
  });

  it('segue funcionando quando o ViaCEP está fora do ar', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url: string) => {
        if (url.includes('viacep')) {
          return Promise.reject(new Error('sem rede'));
        }
        if (url.includes('/bairros')) {
          return Promise.resolve(resposta(BAIRROS));
        }
        if (url.includes('/categorias')) {
          return Promise.resolve(resposta(CATEGORIAS));
        }
        return Promise.resolve(resposta({}));
      }),
    );

    renderizar(<Cadastrar />);

    const cep = await screen.findByLabelText('CEP');
    await userEvent.type(cep, '17011066');
    await userEvent.tab();

    // O formulário continua utilizável: o bairro é escolhido na lista.
    expect(screen.getByLabelText('Bairro')).toBeInTheDocument();
  });

  it('mostra o erro de cada campo embaixo do campo, em português', async () => {
    vi.stubGlobal(
      'fetch',
      respostasPadrao(() =>
        Promise.resolve(
          resposta(
            {
              title: 'Dados inválidos',
              detail: 'Confira os campos destacados e tente de novo.',
              campos: {
                senha: 'A senha precisa ter pelo menos 8 caracteres.',
                documento: 'CPF inválido',
              },
            },
            400,
          ),
        ),
      ),
    );

    renderizar(<Cadastrar />);

    await userEvent.type(await screen.findByLabelText('Seu nome completo'), 'Maria');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar cadastro' }));

    expect(
      await screen.findByText('A senha precisa ter pelo menos 8 caracteres.'),
    ).toBeInTheDocument();
    expect(screen.getByText('CPF inválido')).toBeInTheDocument();
  });

  it('mostra o conflito de e-mail já cadastrado como aviso geral', async () => {
    vi.stubGlobal(
      'fetch',
      respostasPadrao(() =>
        Promise.resolve(
          resposta(
            {
              title: 'Operação em conflito',
              detail: 'Já existe uma conta com esse e-mail. Se for sua, use a opção de entrar.',
            },
            409,
          ),
        ),
      ),
    );

    renderizar(<Cadastrar />);

    await userEvent.click(await screen.findByRole('button', { name: 'Enviar cadastro' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Já existe uma conta');
  });

  it('manda o telefone e o documento só com dígitos, sem a máscara', async () => {
    const espiao = respostasPadrao();
    vi.stubGlobal('fetch', espiao);

    renderizar(<Cadastrar />);

    await userEvent.type(await screen.findByLabelText('Celular com WhatsApp'), '14997123456');
    await userEvent.type(screen.getByLabelText('CPF ou CNPJ'), '52998224725');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar cadastro' }));

    await waitFor(() => {
      const envio = espiao.mock.calls.find(
        (chamada) =>
          String(chamada[0]).includes('/api/cadastro/empreendedores') &&
          chamada[1]?.method === 'POST',
      );
      const corpo = JSON.parse(String(envio![1].body));
      expect(corpo.telefoneWhatsapp).toBe('14997123456');
      expect(corpo.documento).toBe('52998224725');
    });
  });

  it('leva para a política de privacidade antes de enviar', async () => {
    vi.stubGlobal('fetch', respostasPadrao());

    renderizar(<Cadastrar />);

    expect(await screen.findByRole('link', { name: /Como cuidamos deles/ })).toHaveAttribute(
      'href',
      '/privacidade',
    );
  });

  it('não pede foto no cadastro: ela sobe depois, no painel', async () => {
    vi.stubGlobal('fetch', respostasPadrao());

    renderizar(<Cadastrar />);

    await screen.findByLabelText('Seu nome completo');
    expect(screen.queryByLabelText(/foto/i)).toBeNull();
  });
});
