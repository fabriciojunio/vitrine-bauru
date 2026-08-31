package br.com.vitrinebauru.plataforma.web.erros;

/**
 * As quatro maneiras de uma requisicao dar errado por motivo de negocio.
 *
 * <p>Ficam juntas num arquivo so porque sao quatro classes de tres linhas que
 * so existem para o tratador de erro saber qual codigo HTTP devolver. Espalhar
 * isso em quatro arquivos nao acrescentaria nada.
 */
public final class ErrosDeNegocio {

    private ErrosDeNegocio() {
    }

    /**
     * 401: nao provou quem e, ou a prova nao vale mais.
     *
     * <p>Separado do 403 porque a resposta do cliente e outra: aqui o
     * navegador deve mandar o usuario para o login; no 403 ele ja esta logado
     * e mandar de volta para o login so produz um vaivem sem fim.
     */
    public static class NaoAutenticado extends RuntimeException {
        public NaoAutenticado(String mensagem) {
            super(mensagem);
        }
    }

    /** 404: o recurso nao existe, ou nao existe para quem esta perguntando. */
    public static class NaoEncontrado extends RuntimeException {
        public NaoEncontrado(String mensagem) {
            super(mensagem);
        }
    }

    /** 409: o estado atual nao permite a operacao (aprovar quem ja foi aprovado). */
    public static class Conflito extends RuntimeException {
        public Conflito(String mensagem) {
            super(mensagem);
        }
    }

    /** 422: a entrada e valida na forma, mas a regra de negocio recusa. */
    public static class RegraDeNegocio extends RuntimeException {
        public RegraDeNegocio(String mensagem) {
            super(mensagem);
        }
    }

    /**
     * 403: autenticado, mas mexendo no que nao e dele.
     *
     * <p>Diferente de 404 de proposito: aqui quem pede ja provou quem e, e
     * esconder a existencia do recurso nao protege nada, so confunde o suporte.
     */
    public static class Proibido extends RuntimeException {
        public Proibido(String mensagem) {
            super(mensagem);
        }
    }
}
