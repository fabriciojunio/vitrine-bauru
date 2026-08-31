package br.com.vitrinebauru.plataforma.inbox;

import br.com.vitrinebauru.contratos.Evento;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/**
 * Deixa o consumidor idempotente.
 *
 * <p>A entrega do broker e "ao menos uma vez", e a do outbox tambem. Somando
 * as duas, receber o mesmo evento duas vezes nao e falha, e rotina: acontece
 * quando um consumidor demora demais e o grupo e rebalanceado, quando o
 * processo cai entre processar e confirmar, ou quando o publicador reenvia
 * depois de uma queda.
 *
 * <p>A marca e gravada na mesma transacao do trabalho. Se o trabalho falhar, a
 * marca some junto e o evento volta. Se o trabalho der certo, os dois ficam.
 */
@Component
public class Inbox implements RegistroDeEntrada {

    private static final Logger log = LoggerFactory.getLogger(Inbox.class);

    /**
     * Trinta dias guardam o suficiente para cobrir reentrega de verdade sem a
     * tabela crescer para sempre. Reentrega mais velha que isso significa
     * problema grave o bastante para alguem estar olhando de perto.
     */
    private static final Duration RETENCAO = Duration.ofDays(30);

    private final EventoProcessadoRepository repositorio;
    private final MeterRegistry metricas;
    private final Clock relogio;

    public Inbox(EventoProcessadoRepository repositorio, MeterRegistry metricas, Clock relogio) {
        this.repositorio = repositorio;
        this.metricas = metricas;
        this.relogio = relogio;
    }

    /**
     * @return {@code true} se e a primeira vez que este consumidor ve este
     *         evento; {@code false} se ja processou antes e deve ignorar.
     */
    @Override
    public boolean registrar(Evento evento, String consumidor) {
        var chave = new EventoProcessado.Chave(evento.id(), consumidor);
        if (repositorio.existsById(chave)) {
            metricas.counter("vitrine.inbox.repetidos", "consumidor", consumidor).increment();
            log.debug("Evento {} ja processado por {}, ignorando", evento.id(), consumidor);
            return false;
        }
        repositorio.save(new EventoProcessado(evento.id(), consumidor, evento.tipoDoEvento(), relogio.instant()));
        return true;
    }

    @Scheduled(cron = "${vitrine.inbox.cron-expurgo:0 45 3 * * *}", zone = "America/Sao_Paulo")
    @Transactional
    public void expurgarAntigos() {
        int removidos = repositorio.apagarAnterioresA(relogio.instant().minus(RETENCAO));
        if (removidos > 0) {
            log.info("Inbox: {} marcas removidas por retencao", removidos);
        }
    }
}
