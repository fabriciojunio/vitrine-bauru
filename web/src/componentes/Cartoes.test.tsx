import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CartaoDeLoja, CartaoDeProduto, CartaoDeProdutoComContato } from './Cartoes';
import { Foto } from './Foto';
import { BotaoDeWhatsapp } from './BotaoDeWhatsapp';
import { lojaDeExemplo, produtoDeExemplo, renderizarSemSessao, resposta } from '@/testes/ajudantes';

describe('cartão de produto', () => {
  it('mostra nome, preço e a loja de onde vem', () => {
    renderizarSemSessao(<CartaoDeProduto produto={produtoDeExemplo} />);

    expect(screen.getByRole('heading', { name: 'Bolo de pote' })).toBeInTheDocument();
    expect(screen.getByText('R$ 12,00')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Doces da Lourdes' })).toHaveAttribute(
      'href',
      '/loja/doces-da-lourdes',
    );
    expect(screen.getByText('Vila Cardia')).toBeInTheDocument();
  });

  it('mostra sob consulta quando o produto não tem preço', () => {
    renderizarSemSessao(
      <CartaoDeProduto
        produto={{ ...produtoDeExemplo, precoEmCentavos: null, precoFormatado: 'Sob consulta' }}
      />,
    );

    expect(screen.getByText('Sob consulta')).toBeInTheDocument();
  });

  it('não quebra quando a loja ainda não chegou na projeção', () => {
    renderizarSemSessao(
      <CartaoDeProduto produto={{ ...produtoDeExemplo, lojaApelido: null, lojaNome: null }} />,
    );

    expect(screen.getByRole('heading', { name: 'Bolo de pote' })).toBeInTheDocument();
  });

  it('mostra a descrição quando existe', () => {
    renderizarSemSessao(<CartaoDeProduto produto={produtoDeExemplo} />);

    expect(screen.getByText('Massa de chocolate com brigadeiro')).toBeInTheDocument();
  });
});

describe('cartão de loja', () => {
  it('leva para a página da loja pelo nome e pelo botão', () => {
    renderizarSemSessao(<CartaoDeLoja loja={lojaDeExemplo} />);

    const links = screen.getAllByRole('link');
    expect(links.every((link) => link.getAttribute('href') === '/loja/doces-da-lourdes')).toBe(
      true,
    );
  });

  it('mostra categoria e bairro, que é como as pessoas filtram', () => {
    renderizarSemSessao(<CartaoDeLoja loja={lojaDeExemplo} />);

    expect(screen.getByText('Alimentação')).toBeInTheDocument();
    expect(screen.getByText('Vila Cardia')).toBeInTheDocument();
  });
});

describe('foto que ainda não existe', () => {
  it('vira um bloco com as iniciais, e não um retângulo vazio', () => {
    renderizarSemSessao(<Foto url={null} nome="Bolo de pote" categoria="Alimentação" />);

    const marcador = screen.getByRole('img', { name: /foto ainda não enviada/i });
    expect(marcador).toBeInTheDocument();
    expect(marcador).toHaveTextContent('BP');
  });

  it('quando existe, carrega preguiçosa e com texto alternativo', () => {
    renderizarSemSessao(<Foto url="/api/catalogo/imagens/abc" nome="Bolo de pote" />);

    const imagem = screen.getByRole('img', { name: 'Bolo de pote' });
    expect(imagem).toHaveAttribute('loading', 'lazy');
    expect(imagem).toHaveAttribute('src', expect.stringContaining('/api/catalogo/imagens/abc'));
  });

  it('aceita endereço completo sem duplicar o servidor', () => {
    renderizarSemSessao(<Foto url="https://exemplo.invalido/foto.webp" nome="Bolo" />);

    expect(screen.getByRole('img', { name: 'Bolo' })).toHaveAttribute(
      'src',
      'https://exemplo.invalido/foto.webp',
    );
  });
});

describe('botão de falar no WhatsApp', () => {
  it('monta o link com o número e a mensagem pronta', () => {
    renderizarSemSessao(
      <BotaoDeWhatsapp
        empreendedorId="loja-1"
        telefone="(14) 99712-3456"
        nomeDoNegocio="Doces da Lourdes"
        nomeDoProduto="Bolo de pote"
        origem="PAGINA_DO_PRODUTO"
      />,
    );

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', expect.stringContaining('https://wa.me/5514997123456'));
    expect(link.getAttribute('href')).toContain(encodeURIComponent('Bolo de pote'));
  });

  it('abre em outra aba, sem entregar a página de origem', () => {
    renderizarSemSessao(
      <BotaoDeWhatsapp
        empreendedorId="loja-1"
        telefone="14997123456"
        nomeDoNegocio="Doces da Lourdes"
        origem="PAGINA_DA_LOJA"
      />,
    );

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('avisa o servidor no clique, para o contato virar indicador', async () => {
    const espiao = vi.fn().mockResolvedValue(resposta({}));
    vi.stubGlobal('fetch', espiao);

    renderizarSemSessao(
      <BotaoDeWhatsapp
        empreendedorId="loja-1"
        telefone="14997123456"
        nomeDoNegocio="Doces da Lourdes"
        produtoId="produto-1"
        nomeDoProduto="Bolo de pote"
        origem="RESULTADO_DA_BUSCA"
      />,
    );

    await userEvent.click(screen.getByRole('link'));

    expect(espiao).toHaveBeenCalledWith(
      expect.stringContaining('/api/busca/contatos'),
      expect.objectContaining({ method: 'POST' }),
    );
  });

  it('avisa uma vez só, mesmo com vários cliques', async () => {
    const espiao = vi.fn().mockResolvedValue(resposta({}));
    vi.stubGlobal('fetch', espiao);

    renderizarSemSessao(
      <BotaoDeWhatsapp
        empreendedorId="loja-1"
        telefone="14997123456"
        nomeDoNegocio="Doces da Lourdes"
        origem="PAGINA_DA_LOJA"
      />,
    );

    const link = screen.getByRole('link');
    await userEvent.click(link);
    await userEvent.click(link);
    await userEvent.click(link);

    expect(espiao).toHaveBeenCalledTimes(1);
  });

  it('sem nome de produto, a mensagem fala da loja', () => {
    renderizarSemSessao(
      <BotaoDeWhatsapp
        empreendedorId="loja-1"
        telefone="14997123456"
        nomeDoNegocio="Doces da Lourdes"
        origem="PAGINA_DA_LOJA"
      />,
    );

    expect(screen.getByRole('link').getAttribute('href')).toContain(
      encodeURIComponent('Vi a sua loja'),
    );
  });
});

describe('cartão com contato, usado na página da loja', () => {
  it('junta produto e botão de WhatsApp', () => {
    renderizarSemSessao(
      <CartaoDeProdutoComContato
        produto={produtoDeExemplo}
        telefone="(14) 99712-3456"
        nomeDoNegocio="Doces da Lourdes"
      />,
    );

    expect(screen.getByRole('heading', { name: 'Bolo de pote' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Falar no WhatsApp/ })).toBeInTheDocument();
  });
});
