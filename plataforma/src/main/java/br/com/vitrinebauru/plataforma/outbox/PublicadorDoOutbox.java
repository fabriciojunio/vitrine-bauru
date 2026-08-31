package br.com.vitrinebauru.plataforma.outbox;

import br.com.vitrinebauru.plataforma.mensageria.TransporteDeEventos;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/**
 * Lê o outbox e entrega ao transporte.
 *
 * <h2>Publica primeiro, marca depois</h2>
 * A ordem inversa perderia mensagem: se o processo caísse entre marcar e
 * entregar, a mensagem ficaria marcada como publicada sem nunca ter saido.
 * Nesta ordem, uma queda no meio faz a mensagem sair de novo na próxima
 * rodada, o que é aceitável porque o consumidor é idempotente pelo inbox.
 * Entrega ao menos uma vez, nunca zero.
 *
 * <h2>Por que não usar mais threads</h2>
 * Um lote pequeno, sequencial, mantém a transação curta e o lock de linha
 * segurando pouco tempo. Publicar mais rápido não é o gargalo de uma
 * plataforma com algumas centenas de empreendedores; perder evento de
 * aprovação, sim.
 */
@Component
public class PublicadorDoOutbox {

    private static final Logger log = LoggerFactory.getLogger(PublicadorDoOutbox.class);

    private static final int TAMANHO_DO_LOTE = 50;
    private static final Duration RETENCAO_DAS_PUBLICADAS = Duration.ofDays(7);

    private final OutboxRepository repositorio;
    private final TransporteDeEventos transporte;
    private final MeterRegistry metricas;
    private final Clock relogio;

    public PublicadorDoOutbox(OutboxRepository repositorio, TransporteDeEventos transporte,
                              MeterRegistry metricas, Clock relogio) {
        this.repositorio = repositorio;
        this.transporte = transporte;
        this.metricas = metricas;
        this.relogio = relogio;
    }

    @Scheduled(fixedDelayString = "${vitrine.outbox.intervalo-ms:500}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publicarPendentes() {
        var agora = relogio.instant();
        var pendentes = repositorio.proximasParaPublicar(
                agora, MensagemDoOutbox.TENTATIVAS_MAXIMAS, Limit.of(TAMANHO_DO_LOTE));

        for (MensagemDoOutbox mensagem : pendentes) {
            try {
                transporte.enviar(mensagem.topico(), mensagem.chave(), mensagem.carga());
                mensagem.marcarPublicada(relogio.instant());
                metricas.counter("vitrine.outbox.publicadas", "topico", mensagem.topico()).increment();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                mensagem.marcarFalha("envio interrompido", relogio.instant());
                return;

            } catch (Exception e) {
                mensagem.marcarFalha(e.getMessage(), relogio.instant());
                metricas.counter("vitrine.outbox.falhas", "topico", mensagem.topico()).increment();

                if (mensagem.esgotouTentativas()) {
                    log.error("Mensagem {} do tipo {} esgotou {} tentativas e travou. Precisa de analise manual.",
                            mensagem.id(), mensagem.tipo(), MensagemDoOutbox.TENTATIVAS_MAXIMAS, e);
                } else {
                    log.warn("Falha ao publicar {} ({}a tentativa): {}",
                            mensagem.id(), mensagem.tentativas(), e.getMessage());
                }
            }
        }
    }

    /**
     * Outbox crescendo é o primeiro sinal de que o transporte quebrou, e
     * aparece antes de qualquer reclamação de empreendedor. Por isso vira
     * métrica, e não só log.
     */
    @Scheduled(fixedDelayString = "${vitrine.outbox.intervalo-metrica-ms:15000}")
    public void medirPendentes() {
        metricas.gauge("vitrine.outbox.pendentes", repositorio.countByPublicadaEmIsNull());
        metricas.gauge("vitrine.outbox.travadas",
                repositorio.contarTravadas(MensagemDoOutbox.TENTATIVAS_MAXIMAS));
    }

    @Scheduled(cron = "${vitrine.outbox.cron-expurgo:0 30 3 * * *}", zone = "America/Sao_Paulo")
    @Transactional
    public void expurgarPublicadas() {
        int removidas = repositorio.apagarPublicadasAntesDe(
                relogio.instant().minus(RETENCAO_DAS_PUBLICADAS));
        if (removidas > 0) {
            log.info("Outbox: {} mensagens publicadas removidas por retencao", removidas);
        }
    }
}
