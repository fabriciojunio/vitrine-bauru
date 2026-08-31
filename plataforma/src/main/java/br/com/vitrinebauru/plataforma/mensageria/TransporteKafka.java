package br.com.vitrinebauru.plataforma.mensageria;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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

    public TransporteKafka(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void enviar(String topico, String chave, String carga) throws Exception {
        kafka.send(topico, chave, carga).get(ESPERA_DO_ENVIO.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public String descricao() {
        return "kafka";
    }
}
