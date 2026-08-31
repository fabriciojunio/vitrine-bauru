/**
 * O cliente HTTP da aplicação.
 *
 * Duas decisões que valem explicação.
 *
 * O token de acesso vive em memória, e não no armazenamento do navegador. Ele
 * dura quinze minutos e some quando a aba fecha, o que reduz bastante o que um
 * script injetado conseguiria roubar. O de renovação, esse sim, fica no
 * `localStorage`, porque sem ele o empreendedor teria que digitar a senha toda
 * vez que voltasse ao painel. É uma troca assumida, e a defesa contra XSS que
 * a sustenta é o `Content-Security-Policy` do back-end mais a regra de nunca
 * injetar HTML de usuário na tela.
 *
 * A renovação é automática e acontece uma vez só por requisição: quando o
 * servidor responde 401, o cliente troca o par de tokens e repete a chamada
 * original. Sem esse retry, o painel deslogaria sozinho a cada quinze minutos.
 */

export const BASE_DA_API = import.meta.env.VITE_API_URL ?? '';

const CHAVE_DA_RENOVACAO = 'vitrine.renovacao';

let tokenDeAcesso: string | null = null;
let renovacaoEmAndamento: Promise<boolean> | null = null;

export type CamposComErro = Record<string, string>;

/** Erro vindo da API, já no formato que a tela sabe mostrar. */
export class ErroDaApi extends Error {
  readonly status: number;
  readonly campos: CamposComErro;
  readonly correlacao?: string;

  constructor(status: number, mensagem: string, campos: CamposComErro = {}, correlacao?: string) {
    super(mensagem);
    this.name = 'ErroDaApi';
    this.status = status;
    this.campos = campos;
    this.correlacao = correlacao;
  }

  get ehDeAutenticacao(): boolean {
    return this.status === 401;
  }

  /** Servidor fora do ar, endereço não configurado, ou rede caída. */
  get ehDeConexao(): boolean {
    return this.status === 0 || this.status === 404;
  }

  get ehDePermissao(): boolean {
    return this.status === 403;
  }
}

export const sessaoGuardada = {
  guardarAcesso(token: string | null): void {
    tokenDeAcesso = token;
  },
  acesso(): string | null {
    return tokenDeAcesso;
  },
  guardarRenovacao(token: string | null): void {
    try {
      if (token) {
        localStorage.setItem(CHAVE_DA_RENOVACAO, token);
      } else {
        localStorage.removeItem(CHAVE_DA_RENOVACAO);
      }
    } catch {
      // Navegador com armazenamento bloqueado (aba anônima com restrição,
      // por exemplo). A sessão passa a durar só enquanto a aba estiver
      // aberta, o que é bem melhor que a tela quebrar.
    }
  },
  renovacao(): string | null {
    try {
      return localStorage.getItem(CHAVE_DA_RENOVACAO);
    } catch {
      return null;
    }
  },
  limpar(): void {
    tokenDeAcesso = null;
    this.guardarRenovacao(null);
  },
};

interface OpcoesDaChamada {
  metodo?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  corpo?: unknown;
  arquivo?: FormData;
  semAutenticacao?: boolean;
}

async function lerErro(resposta: Response): Promise<ErroDaApi> {
  let detalhe = 'Não foi possível completar a ação. Tente de novo.';
  let campos: CamposComErro = {};
  let correlacao: string | undefined;

  try {
    const corpo = await resposta.json();
    detalhe = corpo.detail ?? corpo.title ?? detalhe;
    campos = corpo.campos ?? {};
    correlacao = corpo.correlacao ?? corpo.ocorrencia;
  } catch {
    // Resposta sem JSON (proxy fora do ar, HTML de erro da hospedagem). A
    // mensagem padrão já cobre, e insistir aqui só esconderia o status.
  }

  return new ErroDaApi(resposta.status, detalhe, campos, correlacao);
}

async function renovarSessao(): Promise<boolean> {
  const renovacao = sessaoGuardada.renovacao();
  if (!renovacao) {
    return false;
  }

  // Uma renovação por vez: várias chamadas falhando juntas não podem virar
  // várias trocas de token, porque a segunda queimaria o token da primeira e
  // o servidor derrubaria a sessão por suspeita de reuso.
  if (!renovacaoEmAndamento) {
    renovacaoEmAndamento = (async () => {
      try {
        const resposta = await fetch(`${BASE_DA_API}/api/cadastro/auth/renovar`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ tokenDeRenovacao: renovacao }),
        });

        if (!resposta.ok) {
          sessaoGuardada.limpar();
          return false;
        }

        const sessao = await resposta.json();
        sessaoGuardada.guardarAcesso(sessao.tokenDeAcesso);
        sessaoGuardada.guardarRenovacao(sessao.tokenDeRenovacao);
        return true;
      } catch {
        return false;
      } finally {
        renovacaoEmAndamento = null;
      }
    })();
  }

  return renovacaoEmAndamento;
}

export async function chamar<T>(caminho: string, opcoes: OpcoesDaChamada = {}): Promise<T> {
  const executar = async (): Promise<Response> => {
    const cabecalhos: Record<string, string> = {};

    if (opcoes.corpo !== undefined) {
      cabecalhos['Content-Type'] = 'application/json';
    }
    const acesso = sessaoGuardada.acesso();
    if (acesso && !opcoes.semAutenticacao) {
      cabecalhos.Authorization = `Bearer ${acesso}`;
    }

    return fetch(`${BASE_DA_API}${caminho}`, {
      method: opcoes.metodo ?? 'GET',
      headers: cabecalhos,
      body: opcoes.arquivo ?? (opcoes.corpo !== undefined ? JSON.stringify(opcoes.corpo) : undefined),
    });
  };

  let resposta: Response;
  try {
    resposta = await executar();
  } catch {
    // Falha de rede: a API não respondeu, ou o endereço dela não foi
    // configurado neste ambiente. É diferente de um erro vindo do servidor, e
    // a tela precisa poder distinguir os dois para explicar o que houve.
    throw new ErroDaApi(0, 'Não foi possível falar com o servidor da plataforma.');
  }

  if (resposta.status === 401 && !opcoes.semAutenticacao && sessaoGuardada.renovacao()) {
    if (await renovarSessao()) {
      try {
        resposta = await executar();
      } catch {
        throw new ErroDaApi(0, 'Não foi possível falar com o servidor da plataforma.');
      }
    }
  }

  if (!resposta.ok) {
    throw await lerErro(resposta);
  }

  if (resposta.status === 204) {
    return undefined as T;
  }

  // Resposta 2xx que não é JSON não é resposta da API: é a hospedagem
  // devolvendo o próprio index.html porque o back-end não está publicado
  // naquele ambiente. Aceitar isso fazia a tela receber uma string onde
  // esperava objeto e quebrar na primeira propriedade lida, com a página
  // inteira em branco e um TypeError no console. Tratar como servidor fora
  // do ar leva para a tela que explica o que houve.
  const tipo = resposta.headers.get('Content-Type') ?? '';
  if (!tipo.includes('json')) {
    throw new ErroDaApi(0, 'Não foi possível falar com o servidor da plataforma.');
  }

  return (await resposta.json()) as T;
}

/**
 * Aviso de contato disparado junto com o clique no link do WhatsApp.
 *
 * Usa `keepalive` porque a aba costuma perder o foco no mesmo instante: sem
 * isso, o navegador cancelaria a requisição na metade e o número de contatos,
 * que é a medida de impacto do projeto, ficaria menor que a realidade.
 */
export function avisarContato(dados: {
  empreendedorId: string;
  produtoId?: string;
  nomeDoProduto?: string;
  origem: 'PAGINA_DO_PRODUTO' | 'PAGINA_DA_LOJA' | 'RESULTADO_DA_BUSCA';
}): void {
  try {
    fetch(`${BASE_DA_API}/api/busca/contatos`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...dados, canal: 'WHATSAPP' }),
      keepalive: true,
    }).catch(() => undefined);
  } catch {
    // Falhar aqui não pode atrapalhar o contato: o link do WhatsApp abre de
    // qualquer jeito, que é o que importa para o empreendedor.
  }
}
