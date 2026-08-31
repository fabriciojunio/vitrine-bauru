package br.com.vitrinebauru.plataforma.mensageria;

import br.com.vitrinebauru.contratos.Evento;

import java.util.Set;

/**
 * Quem reage a um evento.
 *
 * <p>É uma interface, e não a anotação {@code @KafkaListener} espalhada pelos
 * serviços, porque o mesmo consumidor precisa funcionar com broker e sem
 * broker. Quem escreve regra de negócio implementa isto e não fica sabendo
 * qual dos dois está em uso.
 *
 * <p>O método {@link #nome()} vai para o inbox junto com o id do evento. Dois
 * consumidores diferentes do mesmo evento são dois processamentos legítimos, e
 * só o par (evento, consumidor) identifica repetição de verdade.
 */
public interface ConsumidorDeEventos {

    /** Identificador estável. Mudar isto faz eventos já processados voltarem. */
    String nome();

    Set<String> topicos();

    /**
     * Executado dentro de uma transação aberta pelo despachante. Escrever no
     * banco e gravar no outbox aqui dentro cai tudo na mesma transação, que é
     * exatamente o que o padrão exige.
     *
     * <p>Levantar exceção daqui é a forma de dizer "não consegui": a transação
     * volta atrás, o inbox não registra nada e a mensagem é reentregue.
     */
    void consumir(Evento evento);
}
