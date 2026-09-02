package br.com.vitrinebauru.plataforma.mensageria;

/**
 * Traduz o nome do tópico de domínio para o nome do recurso na AWS.
 *
 * <p>Existe por um detalhe chato: os tópicos daqui usam ponto
 * ({@code vitrine.empreendedores}), e nem SNS nem SQS aceitam ponto no nome do
 * recurso. Só letra, número, hífen e sublinhado. Descobrir isso na primeira
 * chamada de criação, com uma mensagem de validação do SDK, custa mais tempo
 * do que esta classe inteira.
 *
 * <p>A tradução mora aqui e não espalhada pelo código porque o nome tem que
 * sair igual em três lugares: na criação do tópico, na criação da fila e na
 * assinatura que liga uma coisa na outra. Se os três não baterem, a fila é
 * criada, a assinatura é criada, nada dá erro, e a mensagem simplesmente não
 * chega.
 */
final class NomesNaAws {

    private NomesNaAws() {
    }

    /** {@code vitrine.empreendedores} vira {@code vitrine-empreendedores}. */
    static String doTopico(String topico) {
        return topico.replace('.', '-');
    }

    /**
     * Nome da fila que atende um grupo.
     *
     * <p>Uma fila por grupo, e não uma por tópico, é o que reproduz o
     * comportamento de grupo de consumo do Kafka: cada serviço tem a própria
     * cópia da mensagem, e uma instância a mais do mesmo serviço divide a
     * mesma fila em vez de processar tudo de novo.
     *
     * <p>O nome do grupo vai como está, sem prefixo. Os grupos deste sistema
     * já nascem com o nome do projeto na frente ({@code vitrine-busca}), e
     * acrescentar outro produziria {@code vitrine-vitrine-busca}.
     */
    static String daFila(String grupo) {
        return grupo.replace('.', '-');
    }

    /** A fila morta segue o nome da fila, para ficar do lado dela na listagem. */
    static String daFilaMorta(String grupo) {
        return daFila(grupo) + "-dlq";
    }
}
