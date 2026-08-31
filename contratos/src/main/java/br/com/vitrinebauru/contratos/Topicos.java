package br.com.vitrinebauru.contratos;

/**
 * Nome dos tópicos do broker.
 *
 * Um tópico por assunto, e não um por serviço: quem publica não precisa saber
 * quem escuta. O sufixo de fila morta segue a convenção do próprio Spring
 * Kafka para não inventar um vocabulário paralelo.
 */
public final class Topicos {

    /** Ciclo de vida do empreendedor: cadastro, aprovação, suspensão. */
    public static final String EMPREENDEDORES = "vitrine.empreendedores";

    /** Produtos publicados, alterados e retirados do catálogo. */
    public static final String CATALOGO = "vitrine.catalogo";

    /** Cliques em "falar no WhatsApp". É a métrica de intenção do produto. */
    public static final String CONTATOS = "vitrine.contatos";

    /** Pedidos de exclusão de dados e as confirmações de cada serviço. */
    public static final String PRIVACIDADE = "vitrine.privacidade";

    public static final String SUFIXO_FILA_MORTA = ".dlq";

    private Topicos() {
    }

    public static String filaMortaDe(String topico) {
        return topico + SUFIXO_FILA_MORTA;
    }
}
