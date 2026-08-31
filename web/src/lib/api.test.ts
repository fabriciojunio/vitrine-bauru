import { beforeEach, describe, expect, it, vi } from 'vitest';
import { avisarContato, chamar, ErroDaApi, sessaoGuardada } from './api';

function resposta(corpo: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers({ 'Content-Type': 'application/json' }),
    json: async () => corpo,
    text: async () => JSON.stringify(corpo),
  } as unknown as Response;
}

function respostaVazia(status: number): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers(),
    json: async () => {
      throw new Error('sem corpo');
    },
    text: async () => '',
  } as unknown as Response;
}

describe('cliente da API', () => {
  beforeEach(() => {
    sessaoGuardada.limpar();
  });

  it('devolve o corpo em JSON quando dá certo', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta({ total: 3 })));

    await expect(chamar('/api/busca/produtos')).resolves.toEqual({ total: 3 });
  });

  it('não manda cabeçalho de autenticação quando não há sessão', async () => {
    const espiao = vi.fn().mockResolvedValue(resposta({}));
    vi.stubGlobal('fetch', espiao);

    await chamar('/api/busca/produtos');

    const cabecalhos = espiao.mock.calls[0]![1].headers;
    expect(cabecalhos.Authorization).toBeUndefined();
  });

  it('manda o token quando há sessão aberta', async () => {
    const espiao = vi.fn().mockResolvedValue(resposta({}));
    vi.stubGlobal('fetch', espiao);
    sessaoGuardada.guardarAcesso('token-de-teste');

    await chamar('/api/cadastro/minha-loja');

    expect(espiao.mock.calls[0]![1].headers.Authorization).toBe('Bearer token-de-teste');
  });

  it('não manda o token quando a chamada é declarada como pública', async () => {
    const espiao = vi.fn().mockResolvedValue(resposta({}));
    vi.stubGlobal('fetch', espiao);
    sessaoGuardada.guardarAcesso('token-de-teste');

    await chamar('/api/cadastro/auth/login', { metodo: 'POST', semAutenticacao: true, corpo: {} });

    expect(espiao.mock.calls[0]![1].headers.Authorization).toBeUndefined();
  });

  it('transforma a resposta de erro do servidor em ErroDaApi legível', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        resposta(
          {
            status: 422,
            title: 'Não foi possível concluir',
            detail: 'Bairro não reconhecido em Bauru. Escolha um da lista.',
            correlacao: 'abc-123',
          },
          422,
        ),
      ),
    );

    await expect(chamar('/api/cadastro/empreendedores')).rejects.toSatisfy((erro: ErroDaApi) => {
      expect(erro).toBeInstanceOf(ErroDaApi);
      expect(erro.status).toBe(422);
      expect(erro.message).toContain('Bairro não reconhecido');
      expect(erro.correlacao).toBe('abc-123');
      return true;
    });
  });

  it('traz os erros por campo, que é o que o formulário mostra embaixo de cada um', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        resposta(
          {
            status: 400,
            title: 'Dados inválidos',
            detail: 'Confira os campos destacados e tente de novo.',
            campos: { nome: 'Escreva o seu nome completo', senha: 'Escolha uma senha' },
          },
          400,
        ),
      ),
    );

    try {
      await chamar('/api/cadastro/empreendedores', { metodo: 'POST', corpo: {} });
      expect.unreachable('deveria ter falhado');
    } catch (erro) {
      expect((erro as ErroDaApi).campos.nome).toBe('Escreva o seu nome completo');
      expect((erro as ErroDaApi).campos.senha).toBe('Escolha uma senha');
    }
  });

  it('não quebra quando o erro vem sem JSON, como um 502 da hospedagem', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respostaVazia(502)));

    await expect(chamar('/api/busca/produtos')).rejects.toThrow(ErroDaApi);
  });

  it('devolve indefinido no 204, que é a resposta de quem apagou algo', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respostaVazia(204)));

    await expect(chamar('/api/catalogo/meus-produtos/1', { metodo: 'DELETE' })).resolves
      .toBeUndefined();
  });

  it('reconhece erro de autenticação e de permissão', () => {
    expect(new ErroDaApi(401, 'x').ehDeAutenticacao).toBe(true);
    expect(new ErroDaApi(403, 'x').ehDePermissao).toBe(true);
    expect(new ErroDaApi(404, 'x').ehDeAutenticacao).toBe(false);
  });
});

describe('renovação automática da sessão', () => {
  beforeEach(() => {
    sessaoGuardada.limpar();
  });

  it('renova e repete a chamada quando o token venceu', async () => {
    sessaoGuardada.guardarAcesso('token-velho');
    sessaoGuardada.guardarRenovacao('renovacao-valida');

    const espiao = vi
      .fn()
      // primeira tentativa: o token venceu
      .mockResolvedValueOnce(resposta({ detail: 'Sessão expirada' }, 401))
      // renovação
      .mockResolvedValueOnce(
        resposta({ tokenDeAcesso: 'token-novo', tokenDeRenovacao: 'renovacao-nova' }),
      )
      // repetição da chamada original
      .mockResolvedValueOnce(resposta({ nomeDoNegocio: 'Doces da Lourdes' }));

    vi.stubGlobal('fetch', espiao);

    const resultado = await chamar<{ nomeDoNegocio: string }>('/api/cadastro/minha-loja');

    expect(resultado.nomeDoNegocio).toBe('Doces da Lourdes');
    expect(espiao).toHaveBeenCalledTimes(3);
    expect(sessaoGuardada.acesso()).toBe('token-novo');
    expect(sessaoGuardada.renovacao()).toBe('renovacao-nova');
  });

  it('limpa a sessão quando a renovação também falha', async () => {
    sessaoGuardada.guardarAcesso('token-velho');
    sessaoGuardada.guardarRenovacao('renovacao-queimada');

    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce(resposta({ detail: 'expirou' }, 401))
        .mockResolvedValueOnce(resposta({ detail: 'uso indevido' }, 401)),
    );

    await expect(chamar('/api/cadastro/minha-loja')).rejects.toThrow(ErroDaApi);
    expect(sessaoGuardada.acesso()).toBeNull();
    expect(sessaoGuardada.renovacao()).toBeNull();
  });

  it('não tenta renovar quando não existe token de renovação', async () => {
    const espiao = vi.fn().mockResolvedValue(resposta({ detail: 'sem sessão' }, 401));
    vi.stubGlobal('fetch', espiao);

    await expect(chamar('/api/cadastro/minha-loja')).rejects.toThrow(ErroDaApi);
    expect(espiao).toHaveBeenCalledTimes(1);
  });

  it('faz uma renovação só quando várias chamadas falham juntas', async () => {
    sessaoGuardada.guardarAcesso('token-velho');
    sessaoGuardada.guardarRenovacao('renovacao-valida');

    const espiao = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/auth/renovar')) {
        return Promise.resolve(
          resposta({ tokenDeAcesso: 'token-novo', tokenDeRenovacao: 'renovacao-nova' }),
        );
      }
      return Promise.resolve(
        sessaoGuardada.acesso() === 'token-novo'
          ? resposta({ certo: true })
          : resposta({ detail: 'expirou' }, 401),
      );
    });
    vi.stubGlobal('fetch', espiao);

    await Promise.all([
      chamar('/api/cadastro/minha-loja'),
      chamar('/api/catalogo/meus-produtos'),
      chamar('/api/cadastro/minha-loja/indicadores'),
    ]);

    const renovacoes = espiao.mock.calls.filter((chamada) =>
      String(chamada[0]).includes('/auth/renovar'),
    );
    expect(renovacoes).toHaveLength(1);
  });
});

describe('sessão guardada', () => {
  it('guarda o acesso só em memória, e a renovação no navegador', () => {
    sessaoGuardada.guardarAcesso('acesso');
    sessaoGuardada.guardarRenovacao('renovacao');

    expect(sessaoGuardada.acesso()).toBe('acesso');
    expect(localStorage.getItem('vitrine.renovacao')).toBe('renovacao');
  });

  it('limpar apaga os dois', () => {
    sessaoGuardada.guardarAcesso('acesso');
    sessaoGuardada.guardarRenovacao('renovacao');

    sessaoGuardada.limpar();

    expect(sessaoGuardada.acesso()).toBeNull();
    expect(sessaoGuardada.renovacao()).toBeNull();
  });
});

describe('aviso de contato', () => {
  it('avisa o servidor com keepalive, para o clique não ser cancelado', () => {
    const espiao = vi.fn().mockResolvedValue(resposta({}));
    vi.stubGlobal('fetch', espiao);

    avisarContato({ empreendedorId: 'loja-1', origem: 'PAGINA_DO_PRODUTO' });

    expect(espiao).toHaveBeenCalledWith(
      expect.stringContaining('/api/busca/contatos'),
      expect.objectContaining({ keepalive: true, method: 'POST' }),
    );
  });

  it('engole a falha: o WhatsApp precisa abrir mesmo sem o registro', () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockRejectedValue(new Error('rede fora do ar')),
    );

    expect(() =>
      avisarContato({ empreendedorId: 'loja-1', origem: 'PAGINA_DA_LOJA' }),
    ).not.toThrow();
  });
});
