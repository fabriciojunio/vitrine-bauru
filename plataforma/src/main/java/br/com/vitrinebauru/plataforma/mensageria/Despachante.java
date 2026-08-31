package br.com.vitrinebauru.plataforma.mensageria;

import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.plataforma.inbox.RegistroDeEntrada;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Entrega o evento a quem assinou o topico, uma transacao por consumidor.
 *
 * <p>E o mesmo caminho com broker e sem broker. O transporte muda o jeito de a
 * mensagem chegar aqui; daqui para a frente, idempotencia, transacao e
 * tratamento de falha sao identicos.
 *
 * <h2>Uma transacao por consumidor, e nao uma para todos</h2>
 * Se dois consumidores reagem ao mesmo evento e o segundo falha, o trabalho do
 * primeiro nao deve ser desfeito: ele deu certo. Com transacoes separadas, a
 * reentrega refaz so o que falhou, e o inbox faz o que ja deu certo ser
 * pulado.
 */
@Component
public class Despachante {

    private static final Logger log = LoggerFactory.getLogger(Despachante.class);

    private final List<ConsumidorDeEventos> consumidores;
    private final MapeadorDeEventos mapeador;
    private final RegistroDeEntrada inbox;
    private final TransactionTemplate transacao;
    private final MeterRegistry metricas;

    public Despachante(List<ConsumidorDeEventos> consumidores,
                       MapeadorDeEventos mapeador,
                       RegistroDeEntrada inbox,
                       PlatformTransactionManager gerenteDeTransacao,
                       MeterRegistry metricas) {
        this.consumidores = consumidores;
        this.mapeador = mapeador;
        this.inbox = inbox;
        this.metricas = metricas;

        this.transacao = new TransactionTemplate(gerenteDeTransacao);
        // Sem broker, quem chama isto e o publicador do outbox, que ja esta
        // dentro de uma transacao. Sem REQUIRES_NEW, uma falha do consumidor
        // marcaria a transacao inteira para desfazer e o proprio registro da
        // tentativa que falhou seria perdido, deixando a mensagem em retry
        // infinito sem nunca contar as tentativas.
        this.transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void despachar(String topico, String carga) {
        Evento evento = mapeador.deJson(carga);
        MDC.put("correlacao", evento.correlacao().toString());
        MDC.put("evento", evento.tipoDoEvento());
        try {
            for (ConsumidorDeEventos consumidor : consumidoresDe(topico)) {
                entregar(consumidor, evento);
            }
        } finally {
            MDC.remove("correlacao");
            MDC.remove("evento");
        }
    }

    private void entregar(ConsumidorDeEventos consumidor, Evento evento) {
        Timer.Sample cronometro = Timer.start();
        transacao.executeWithoutResult(situacao -> {
            if (!inbox.registrar(evento, consumidor.nome())) {
                return;
            }
            consumidor.consumir(evento);
            log.debug("{} tratou {} ({})", consumidor.nome(), evento.tipoDoEvento(), evento.id());
        });
        cronometro.stop(metricas.timer("vitrine.evento.consumo",
                "consumidor", consumidor.nome(), "evento", evento.tipoDoEvento()));
    }

    private List<ConsumidorDeEventos> consumidoresDe(String topico) {
        return consumidores.stream().filter(consumidor -> consumidor.topicos().contains(topico)).toList();
    }

    /** Topicos que este servico precisa escutar. Vazio significa nao assinar nada. */
    public Set<String> topicosAssinados() {
        Set<String> topicos = new TreeSet<>();
        consumidores.forEach(consumidor -> topicos.addAll(consumidor.topicos()));
        return topicos;
    }
}
