package br.com.vitrinebauru.plataforma.mensageria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Liga o consumo do broker aos consumidores registrados no serviço.
 *
 * <p>O container é montado a mão, em vez de usar {@code @KafkaListener}, por
 * uma razão concreta: a lista de tópicos só é conhecida em tempo de execução,
 * somando o que cada {@link ConsumidorDeEventos} do serviço assinou. A
 * anotação exigiria repetir essa lista num lugar que ninguém lembra de
 * atualizar ao acrescentar um consumidor.
 *
 * <p>Serviço sem nenhum consumidor (o cadastro publica muito e escuta pouco)
 * simplesmente não abre conexão de consumo.
 */
@Component
@ConditionalOnProperty(name = "vitrine.mensageria.transporte", havingValue = "kafka", matchIfMissing = true)
public class OuvinteDoKafka implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(OuvinteDoKafka.class);

    private final ConsumerFactory<String, String> fabrica;
    private final Despachante despachante;
    private final CommonErrorHandler tratadorDeErro;
    private final String grupo;

    private ConcurrentMessageListenerContainer<String, String> container;

    public OuvinteDoKafka(ConsumerFactory<String, String> fabrica,
                          Despachante despachante,
                          CommonErrorHandler tratadorDeErro,
                          @Value("${vitrine.mensageria.grupo}") String grupo) {
        this.fabrica = fabrica;
        this.despachante = despachante;
        this.tratadorDeErro = tratadorDeErro;
        this.grupo = grupo;
    }

    @Override
    public void start() {
        Set<String> topicos = despachante.topicosAssinados();
        if (topicos.isEmpty()) {
            log.info("Nenhum consumidor registrado: este servico so publica");
            return;
        }

        var propriedades = new ContainerProperties(topicos.toArray(String[]::new));
        propriedades.setGroupId(grupo);
        propriedades.setMessageListener((MessageListener<String, String>) registro ->
                despachante.despachar(registro.topic(), registro.value()));

        container = new ConcurrentMessageListenerContainer<>(fabrica, propriedades);
        container.setBeanName("ouvinte-" + grupo);
        container.setCommonErrorHandler(tratadorDeErro);
        container.start();

        log.info("Escutando {} no grupo {}", topicos, grupo);
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    @Override
    public boolean isRunning() {
        return container != null && container.isRunning();
    }

    /**
     * Sobe depois dos outros componentes e desce antes deles: começar a
     * consumir com metade do contexto pronto é receita de erro que não se
     * reproduz.
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}
