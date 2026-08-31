package br.com.vitrinebauru.plataforma.mensageria;

import br.com.vitrinebauru.contratos.Topicos;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuracao do broker.
 *
 * <p>So entra em cena quando o transporte e o Kafka. Na implantacao gratuita
 * nada disto e criado, e o servico sobe sem nenhuma dependencia de broker.
 */
@Configuration
@ConditionalOnProperty(name = "vitrine.mensageria.transporte", havingValue = "kafka", matchIfMissing = true)
public class ConfiguracaoDaMensageria {

    /** Tres tentativas com um segundo de espera, e depois a fila morta. */
    private static final long ESPERA_ENTRE_TENTATIVAS_MS = 1000L;
    private static final long TENTATIVAS = 3L;

    @Bean
    public ProducerFactory<String, String> fabricaDeProdutor(KafkaProperties propriedades) {
        Map<String, Object> configuracao = new HashMap<>(propriedades.buildProducerProperties(null));
        configuracao.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configuracao.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Confirmacao de todas as replicas: perder evento de aprovacao de
        // cadastro custa mais caro que os milissegundos de espera.
        configuracao.put(ProducerConfig.ACKS_CONFIG, "all");
        configuracao.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configuracao);
    }

    @Bean
    public KafkaTemplate<String, String> modeloDoKafka(ProducerFactory<String, String> fabrica) {
        return new KafkaTemplate<>(fabrica);
    }

    @Bean
    public ConsumerFactory<String, String> fabricaDeConsumidor(KafkaProperties propriedades,
                                                               @Value("${vitrine.mensageria.grupo}") String grupo) {
        Map<String, Object> configuracao = new HashMap<>(propriedades.buildConsumerProperties(null));
        configuracao.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configuracao.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configuracao.put(ConsumerConfig.GROUP_ID_CONFIG, grupo);
        // Comeca do inicio quando o grupo e novo: um servico que sobe pela
        // primeira vez precisa ver o que ja aconteceu, senao a projecao da
        // busca nasce vazia e ninguem entende por que a loja sumiu.
        configuracao.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configuracao.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(configuracao);
    }

    /**
     * Depois de tres tentativas a mensagem vai para a fila morta do topico, em
     * vez de bloquear a particao para sempre. Mensagem envenenada trava fila,
     * e fila travada e o jeito silencioso de um sistema de eventos parar.
     */
    @Bean
    public CommonErrorHandler tratadorDeErroDoConsumo(KafkaTemplate<String, String> kafka) {
        var encaminhador = new DeadLetterPublishingRecoverer(kafka,
                (registro, excecao) -> new TopicPartition(
                        Topicos.filaMortaDe(registro.topic()), registro.partition()));
        return new DefaultErrorHandler(encaminhador,
                new FixedBackOff(ESPERA_ENTRE_TENTATIVAS_MS, TENTATIVAS));
    }

    @Bean
    public NewTopic topicoDeEmpreendedores() {
        return TopicBuilder.name(Topicos.EMPREENDEDORES).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicoDeCatalogo() {
        return TopicBuilder.name(Topicos.CATALOGO).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicoDeContatos() {
        return TopicBuilder.name(Topicos.CONTATOS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicoDePrivacidade() {
        return TopicBuilder.name(Topicos.PRIVACIDADE).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic filaMortaDeEmpreendedores() {
        return TopicBuilder.name(Topicos.filaMortaDe(Topicos.EMPREENDEDORES)).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic filaMortaDeCatalogo() {
        return TopicBuilder.name(Topicos.filaMortaDe(Topicos.CATALOGO)).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic filaMortaDeContatos() {
        return TopicBuilder.name(Topicos.filaMortaDe(Topicos.CONTATOS)).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic filaMortaDePrivacidade() {
        return TopicBuilder.name(Topicos.filaMortaDe(Topicos.PRIVACIDADE)).partitions(3).replicas(1).build();
    }
}
