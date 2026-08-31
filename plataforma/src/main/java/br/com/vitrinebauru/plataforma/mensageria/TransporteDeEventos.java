package br.com.vitrinebauru.plataforma.mensageria;

/**
 * Por onde a mensagem sai do processo.
 *
 * <p>Esta interface é a fronteira que permite o mesmo código rodar em dois
 * formatos de implantação. Em ambiente de verdade existe um broker e o
 * transporte é o Kafka. Na implantação gratuita, que é a que a SEDECON vai
 * abrir no navegador, não existe broker nenhum: em 2026 não há Kafka
 * gerenciado com camada gratuita permanente, e o projeto é de graduação, sem
 * orçamento. Nesse caso o transporte entrega o evento ao mesmo despachante
 * dentro do próprio processo.
 *
 * <p>O que não muda entre os dois: o evento passa pelo outbox antes, o
 * consumidor é idempotente pelo inbox depois, e a falha volta para o outbox
 * tentar de novo. Trocar o transporte troca a rede, não as garantias.
 *
 * <p>Ver docs/adr/0002-transporte-de-eventos.md.
 */
public interface TransporteDeEventos {

    /**
     * Entrega a mensagem, ou levanta exceção. Síncrono de propósito: quem
     * chama é o publicador do outbox, e ele só pode marcar a mensagem como
     * publicada depois que a entrega foi confirmada.
     */
    void enviar(String topico, String chave, String carga) throws Exception;

    /** Nome curto para log e métrica. */
    String descricao();
}
