package br.com.vitrinebauru.plataforma.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Marca de que um evento já foi tratado por um consumidor.
 *
 * <p>A chave é o par (evento, consumidor), e não só o evento: dentro do mesmo
 * serviço, dois consumidores diferentes podem reagir ao mesmo
 * {@code CadastroAprovado}, e isso não é repetição, são dois trabalhos
 * distintos.
 */
@Entity
@Table(name = "inbox")
public class EventoProcessado {

    @EmbeddedId
    private Chave chave;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "processado_em", nullable = false)
    private Instant processadoEm;

    protected EventoProcessado() {
    }

    public EventoProcessado(UUID eventoId, String consumidor, String tipo, Instant processadoEm) {
        this.chave = new Chave(eventoId, consumidor);
        this.tipo = tipo;
        this.processadoEm = processadoEm;
    }

    public UUID eventoId() {
        return chave.eventoId();
    }

    public String consumidor() {
        return chave.consumidor();
    }

    public String tipo() {
        return tipo;
    }

    public Instant processadoEm() {
        return processadoEm;
    }

    @Embeddable
    public static class Chave implements Serializable {

        @Column(name = "evento_id", nullable = false)
        private UUID eventoId;

        @Column(name = "consumidor", nullable = false)
        private String consumidor;

        protected Chave() {
        }

        public Chave(UUID eventoId, String consumidor) {
            this.eventoId = eventoId;
            this.consumidor = consumidor;
        }

        public UUID eventoId() {
            return eventoId;
        }

        public String consumidor() {
            return consumidor;
        }

        @Override
        public boolean equals(Object outro) {
            if (this == outro) {
                return true;
            }
            if (!(outro instanceof Chave chave)) {
                return false;
            }
            return Objects.equals(eventoId, chave.eventoId) && Objects.equals(consumidor, chave.consumidor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventoId, consumidor);
        }
    }
}
