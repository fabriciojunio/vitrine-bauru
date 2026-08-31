/** Os formatos que a API devolve, do jeito que a tela consome. */

export interface UsuarioLogado {
  id: string;
  nome: string;
  email: string;
  papel: 'EMPREENDEDOR' | 'ADMIN_SEDECON';
  empreendedorId: string | null;
}

export interface Sessao {
  tokenDeAcesso: string;
  tokenDeRenovacao: string;
  expiraEm: string;
  usuario: UsuarioLogado;
}

export interface Pagina<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  total: number;
  totalDePaginas: number;
  temProxima: boolean;
}

export interface ProdutoNaVitrine {
  id: string;
  nome: string;
  descricao: string | null;
  precoEmCentavos: number | null;
  precoFormatado: string;
  categoria: string;
  imagemUrl: string | null;
  empreendedorId: string;
  lojaNome: string | null;
  lojaApelido: string | null;
  bairro: string | null;
}

export interface LojaNaVitrine {
  id: string;
  nomeDoNegocio: string;
  apelidoNaUrl: string;
  descricao: string | null;
  categoria: string;
  bairro: string;
  telefoneWhatsapp: string;
  fotoDeCapaUrl: string | null;
}

export interface LojaCompleta {
  loja: LojaNaVitrine;
  produtos: ProdutoNaVitrine[];
}

export interface ResumoDaVitrine {
  lojas: number;
  produtos: number;
  bairros: string[];
  categorias: string[];
}

export interface MinhaLoja {
  id: string;
  nomeDoNegocio: string;
  apelidoNaUrl: string;
  descricao: string | null;
  categoriaPrincipal: string;
  bairro: string;
  cep: string | null;
  telefoneWhatsapp: string;
  documento: string;
  fotoDeCapaUrl: string | null;
  situacao: 'PENDENTE' | 'APROVADO' | 'REJEITADO' | 'SUSPENSO' | 'EXCLUIDO';
  motivoDaModeracao: string | null;
  cadastradoEm: string;
  moderadoEm: string | null;
  apareceNaVitrine: boolean;
}

export interface ProdutoDoPainel {
  id: string;
  nome: string;
  descricao: string | null;
  precoEmCentavos: number | null;
  precoFormatado: string;
  categoriaId: string;
  imagemUrl: string | null;
  disponivel: boolean;
  criadoEm: string;
}

export interface CategoriaDoCatalogo {
  id: string;
  nome: string;
  slug: string;
}

export interface CadastroParaAnalise {
  id: string;
  nomeDoNegocio: string;
  descricao: string | null;
  categoriaPrincipal: string;
  bairro: string;
  telefoneWhatsapp: string;
  documento: string;
  tipoDoDocumento: string;
  situacaoDoDocumento: string | null;
  situacao: string;
  cadastradoEm: string;
  diasNaFila: number;
}

export interface PainelDaSedecon {
  empreendedoresAprovados: number;
  cadastrosPendentes: number;
  empreendedoresSuspensos: number;
  cadastrosRejeitados: number;
  produtosPublicados: number;
  contatosNoTotal: number;
  contatosNosUltimos30Dias: number;
  aprovadosSemNenhumProduto: number;
  aprovadosPorBairro: Record<string, number>;
  maisProcurados: { empreendedorId: string; nomeDoNegocio: string; contatos: number }[];
  precisamDeAjuda: {
    empreendedorId: string;
    nomeDoNegocio: string;
    bairro: string;
    aprovadoEm: string | null;
  }[];
}

export interface IndicadoresDoEmpreendedor {
  produtos: number;
  contatosNoTotal: number;
  contatosNosUltimos30Dias: number;
}
