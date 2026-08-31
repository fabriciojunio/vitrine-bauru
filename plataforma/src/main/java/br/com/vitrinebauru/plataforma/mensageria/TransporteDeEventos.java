package br.com.vitrinebauru.plataforma.mensageria;

/**
 * Por onde a mensagem sai do processo.
 *
 * <p>Esta interface e a fronteira que permite o mesmo codigo rodar em dois
 * formatos de implantacao. Em ambiente de verdade existe um broker e o
 * transporte e o Kafka. Na implantacao gratuita, que e a que a SEDECON vai
 * abrir no navegador, nao existe broker nenhum: em 2026 nao ha Kafka
 * gerenciado com camada gratuita permanente, e o projeto e de graduacao, sem
 * orcamento. Nesse caso o transporte entrega o evento ao mesmo despachante
 * dentro do proprio processo.
 *
 * <p>O que nao muda entre os dois: o evento passa pelo outbox antes, o
 * consumidor e idempotente pelo inbox depois, e a falha volta para o outbox
 * tentar de novo. Trocar o transporte troca a rede, nao as garantias.
 *
 * <p>Ver docs/adr/0002-transporte-de-eventos.md.
 */
public interface TransporteDeEventos {

    /**
     * Entrega a mensagem, ou levanta excecao. Sincrono de proposito: quem
     * chama e o publicador do outbox, e ele so pode marcar a mensagem como
     * publicada depois que a entrega foi confirmada.
     */
    void enviar(String topico, String chave, String carga) throws Exception;

    /** Nome curto para log e metrica. */
    String descricao();
}
