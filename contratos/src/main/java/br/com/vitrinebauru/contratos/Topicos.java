package br.com.vitrinebauru.contratos;

/**
 * Nome dos topicos do broker.
 *
 * Um topico por assunto, e nao um por servico: quem publica nao precisa saber
 * quem escuta. O sufixo de fila morta segue a convencao do proprio Spring
 * Kafka para nao inventar um vocabulario paralelo.
 */
public final class Topicos {

    /** Ciclo de vida do empreendedor: cadastro, aprovacao, suspensao. */
    public static final String EMPREENDEDORES = "vitrine.empreendedores";

    /** Produtos publicados, alterados e retirados do catalogo. */
    public static final String CATALOGO = "vitrine.catalogo";

    /** Cliques em "falar no WhatsApp". E a metrica de intencao do produto. */
    public static final String CONTATOS = "vitrine.contatos";

    /** Pedidos de exclusao de dados e as confirmacoes de cada servico. */
    public static final String PRIVACIDADE = "vitrine.privacidade";

    public static final String SUFIXO_FILA_MORTA = ".dlq";

    private Topicos() {
    }

    public static String filaMortaDe(String topico) {
        return topico + SUFIXO_FILA_MORTA;
    }
}
