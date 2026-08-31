package br.com.vitrinebauru.plataforma.web.erros;

/**
 * As quatro maneiras de uma requisição dar errado por motivo de negócio.
 *
 * <p>Ficam juntas num arquivo só porque são quatro classes de três linhas que
 * só existem para o tratador de erro saber qual código HTTP devolver. Espalhar
 * isso em quatro arquivos não acrescentaria nada.
 */
public final class ErrosDeNegocio {

    private ErrosDeNegocio() {
    }

    /**
     * 401: não provou quem é, ou a prova não vale mais.
     *
     * <p>Separado do 403 porque a resposta do cliente é outra: aqui o
     * navegador deve mandar o usuário para o login; no 403 ele já está logado
     * e mandar de volta para o login só produz um vaivém sem fim.
     */
    public static class NaoAutenticado extends RuntimeException {
        public NaoAutenticado(String mensagem) {
            super(mensagem);
        }
    }

    /** 404: o recurso não existe, ou não existe para quem está perguntando. */
    public static class NaoEncontrado extends RuntimeException {
        public NaoEncontrado(String mensagem) {
            super(mensagem);
        }
    }

    /** 409: o estado atual não permite a operação (aprovar quem já foi aprovado). */
    public static class Conflito extends RuntimeException {
        public Conflito(String mensagem) {
            super(mensagem);
        }
    }

    /** 422: a entrada é valida na forma, mas a regra de negócio recusa. */
    public static class RegraDeNegocio extends RuntimeException {
        public RegraDeNegocio(String mensagem) {
            super(mensagem);
        }
    }

    /**
     * 403: autenticado, mas mexendo no que não é dele.
     *
     * <p>Diferente de 404 de propósito: aqui quem pede já provou quem é, e
     * esconder a existência do recurso não protege nada, só confunde o suporte.
     */
    public static class Proibido extends RuntimeException {
        public Proibido(String mensagem) {
            super(mensagem);
        }
    }
}
