package br.com.vitrinebauru.plataforma.mensageria;

import br.com.vitrinebauru.plataforma.observabilidade.RastroDaMensagem;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Transporte de produção: publica no broker.
 *
 * <p>O {@code get()} no retorno do envio deixa a publicação síncrona de
 * propósito. Assíncrono seria mais rápido e marcaria como publicada uma
 * mensagem cujo envio ainda podia falhar; aqui a vazão vale menos que a
 * garantia.
 */
@Component
@ConditionalOnProperty(name = "vitrine.mensageria.transporte", havingValue = "kafka", matchIfMissing = true)
public class TransporteKafka implements TransporteDeEventos {

    private static final Duration ESPERA_DO_ENVIO = Duration.ofSeconds(10);

    private final KafkaTemplate<String, String> kafka;
    private final RastroDaMensagem rastro;

    public TransporteKafka(KafkaTemplate<String, String> kafka, RastroDaMensagem rastro) {
        this.kafka = kafka;
        this.rastro = rastro;
    }

    @Override
    public void enviar(String topico, String chave, String carga) throws Exception {
        var registro = new ProducerRecord<>(topico, chave, carga);

        // O rastro viaja em cabeçalho, e não dentro da carga, porque a carga é
        // o contrato entre serviços: quem consome não deveria precisar
        // desserializar dado de observabilidade para entender o evento.
        String traceparent = rastro.capturar();
        if (traceparent != null) {
            registro.headers().add(RastroDaMensagem.CAMPO,
                    traceparent.getBytes(StandardCharsets.UTF_8));
        }

        kafka.send(registro).get(ESPERA_DO_ENVIO.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public String descricao() {
        return "kafka";
    }
}
