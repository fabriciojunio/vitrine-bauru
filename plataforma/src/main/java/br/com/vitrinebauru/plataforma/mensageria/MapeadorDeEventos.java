package br.com.vitrinebauru.plataforma.mensageria;

import br.com.vitrinebauru.contratos.Evento;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

/**
 * Converte evento em JSON e de volta.
 *
 * <p>Usa um {@code ObjectMapper} próprio, e não o do Spring, por um motivo de
 * compatibilidade: quem configura o mapeador da aplicação mexe no formato da
 * API REST, e mexer no formato da API não pode reescrever o formato das
 * mensagens que já estão gravadas no outbox esperando para sair.
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES} desligado de propósito: durante uma
 * implantação os serviços ficam alguns minutos em versões diferentes, e o
 * serviço antigo precisa conseguir ler um evento que ganhou campo novo. Campo
 * novo pode ser ignorado; campo removido, não, e por isso a regra do projeto é
 * só acrescentar campo em evento.
 */
@Component
public class MapeadorDeEventos {

    private final ObjectMapper mapeador;

    public MapeadorDeEventos() {
        this.mapeador = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String paraJson(Evento evento) {
        try {
            return mapeador.writeValueAsString(evento);
        } catch (Exception e) {
            throw new EventoIlegivel("Nao foi possivel serializar o evento " + evento.tipoDoEvento(), e);
        }
    }

    public Evento deJson(String json) {
        try {
            return mapeador.readValue(json, Evento.class);
        } catch (Exception e) {
            throw new EventoIlegivel("Nao foi possivel ler o evento recebido", e);
        }
    }

    public static class EventoIlegivel extends RuntimeException {
        public EventoIlegivel(String mensagem, Throwable causa) {
            super(mensagem, causa);
        }
    }
}
