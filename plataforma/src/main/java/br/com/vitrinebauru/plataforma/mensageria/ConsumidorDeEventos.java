package br.com.vitrinebauru.plataforma.mensageria;

import br.com.vitrinebauru.contratos.Evento;

import java.util.Set;

/**
 * Quem reage a um evento.
 *
 * <p>E uma interface, e nao a anotacao {@code @KafkaListener} espalhada pelos
 * servicos, porque o mesmo consumidor precisa funcionar com broker e sem
 * broker. Quem escreve regra de negocio implementa isto e nao fica sabendo
 * qual dos dois esta em uso.
 *
 * <p>O metodo {@link #nome()} vai para o inbox junto com o id do evento. Dois
 * consumidores diferentes do mesmo evento sao dois processamentos legitimos, e
 * so o par (evento, consumidor) identifica repeticao de verdade.
 */
public interface ConsumidorDeEventos {

    /** Identificador estavel. Mudar isto faz eventos ja processados voltarem. */
    String nome();

    Set<String> topicos();

    /**
     * Executado dentro de uma transacao aberta pelo despachante. Escrever no
     * banco e gravar no outbox aqui dentro cai tudo na mesma transacao, que e
     * exatamente o que o padrao exige.
     *
     * <p>Levantar excecao daqui e a forma de dizer "nao consegui": a transacao
     * volta atras, o inbox nao registra nada e a mensagem e reentregue.
     */
    void consumir(Evento evento);
}
