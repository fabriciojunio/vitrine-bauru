import { describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Arco, AreaDeTexto, Aviso, Botao, Campo, Carregando, Selecao, TituloDeSecao } from './Basicos';

describe('Botão', () => {
  it('mostra o texto e responde ao clique', async () => {
    const clicar = vi.fn();
    render(<Botao onClick={clicar}>Procurar</Botao>);

    await userEvent.click(screen.getByRole('button', { name: 'Procurar' }));

    expect(clicar).toHaveBeenCalledOnce();
  });

  it('não deixa clicar duas vezes enquanto está enviando', async () => {
    const clicar = vi.fn();
    render(
      <Botao onClick={clicar} carregando>
        Enviar cadastro
      </Botao>,
    );

    const botao = screen.getByRole('button');
    expect(botao).toBeDisabled();
    expect(botao).toHaveAttribute('aria-busy', 'true');
    expect(botao).toHaveTextContent('Aguarde');

    await userEvent.click(botao);
    expect(clicar).not.toHaveBeenCalled();
  });

  it.each(['principal', 'selo', 'neutro', 'texto'] as const)(
    'aplica a variante %s',
    (variante) => {
      render(<Botao variante={variante}>Ação</Botao>);
      expect(screen.getByRole('button')).toHaveClass(`botao-${variante}`);
    },
  );
});

describe('Campo de texto', () => {
  it('liga a etiqueta ao campo, para o leitor de tela e para o clique na etiqueta', () => {
    render(<Campo etiqueta="Seu e-mail" />);

    expect(screen.getByLabelText('Seu e-mail')).toBeInTheDocument();
  });

  it('mostra o texto de ajuda e o associa ao campo', () => {
    render(<Campo etiqueta="CEP" ajuda="Preenche o bairro sozinho." />);

    const campo = screen.getByLabelText('CEP');
    expect(screen.getByText('Preenche o bairro sozinho.')).toBeInTheDocument();
    expect(campo).toHaveAttribute('aria-describedby');
  });

  it('anuncia o erro como alerta e marca o campo como inválido', () => {
    render(<Campo etiqueta="Senha" erro="Escolha uma senha com 8 caracteres" />);

    expect(screen.getByRole('alert')).toHaveTextContent('Escolha uma senha com 8 caracteres');
    expect(screen.getByLabelText('Senha')).toHaveAttribute('aria-invalid', 'true');
  });

  it('não fica inválido quando não há erro', () => {
    render(<Campo etiqueta="Nome" />);

    expect(screen.getByLabelText('Nome')).not.toHaveAttribute('aria-invalid');
  });
});

describe('Área de texto', () => {
  it('conta os caracteres que faltam', () => {
    render(
      <AreaDeTexto
        etiqueta="O que você faz"
        value="Bolo de pote"
        maxLength={600}
        onChange={() => undefined}
      />,
    );

    expect(screen.getByText('12/600')).toBeInTheDocument();
  });

  it('avisa o erro em texto, e não só em cor', () => {
    render(
      <AreaDeTexto
        etiqueta="Descrição"
        value=""
        erro="Escreva o que você vende"
        onChange={() => undefined}
      />,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Escreva o que você vende');
  });
});

describe('Seleção', () => {
  it('lista as opções com um item vazio para quem ainda não escolheu', () => {
    render(
      <Selecao
        etiqueta="Bairro"
        vazio="Todos os bairros"
        opcoes={['Centro', 'Vila Cardia']}
        onChange={() => undefined}
      />,
    );

    expect(screen.getByRole('option', { name: 'Todos os bairros' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Vila Cardia' })).toBeInTheDocument();
  });

  it('avisa a escolha de quem seleciona', async () => {
    const trocar = vi.fn();
    render(
      <Selecao
        etiqueta="Categoria"
        vazio="Escolha"
        opcoes={['Alimentação', 'Pet']}
        onChange={trocar}
      />,
    );

    await userEvent.selectOptions(screen.getByLabelText('Categoria'), 'Pet');

    expect(trocar).toHaveBeenCalled();
  });
});

describe('Aviso', () => {
  it('erro é anunciado como alerta, para o leitor de tela interromper', () => {
    render(<Aviso tipo="erro">Não foi possível entrar</Aviso>);

    expect(screen.getByRole('alert')).toHaveTextContent('Não foi possível entrar');
  });

  it('aviso comum é anunciado como estado, sem interromper', () => {
    render(<Aviso tipo="certo">Sua loja está no ar</Aviso>);

    expect(screen.getByRole('status')).toHaveTextContent('Sua loja está no ar');
  });

  it('mostra o título junto do texto', () => {
    render(
      <Aviso tipo="atencao" titulo="Seu cadastro está na fila">
        A análise leva alguns dias.
      </Aviso>,
    );

    expect(screen.getByText('Seu cadastro está na fila')).toBeInTheDocument();
    expect(screen.getByText('A análise leva alguns dias.')).toBeInTheDocument();
  });
});

describe('elementos de identidade', () => {
  it('o arco é decorativo e fica escondido do leitor de tela', () => {
    const { container } = render(<Arco />);

    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
  });

  it('o título de seção sai como cabeçalho de verdade', () => {
    render(<TituloDeSecao descricao="O que está disponível agora.">Na vitrine hoje</TituloDeSecao>);

    expect(screen.getByRole('heading', { name: 'Na vitrine hoje' })).toBeInTheDocument();
    expect(screen.getByText('O que está disponível agora.')).toBeInTheDocument();
  });

  it('o carregando é anunciado como estado', () => {
    render(<Carregando texto="Procurando…" />);

    expect(screen.getByRole('status')).toHaveTextContent('Procurando…');
  });

  it('não explica a hibernação nos primeiros segundos', () => {
    // Na maioria das visitas a resposta chega rápido, e falar de hibernação
    // para quem esperou meio segundo cria dúvida onde não havia.
    vi.useFakeTimers();
    render(<Carregando texto="Procurando…" />);

    expect(screen.queryByText(/hiberna/)).toBeNull();
    vi.useRealTimers();
  });

  it('explica a espera quando ela passa de quatro segundos', async () => {
    // Aconteceu em produção: a API em camada gratuita hiberna e a primeira
    // visita esperou 91 segundos. A tela mostrava só "Procurando…", e quem
    // abriu concluiu que estava quebrado.
    vi.useFakeTimers();
    render(<Carregando texto="Procurando…" />);

    await act(async () => {
      vi.advanceTimersByTime(4100);
    });

    expect(screen.getByText(/O servidor hiberna/)).toBeInTheDocument();
    expect(screen.getByText(/não precisa recarregar/)).toBeInTheDocument();
    vi.useRealTimers();
  });
});
