package br.com.vitrinebauru.notificacoes.aplicacao;

import br.com.vitrinebauru.notificacoes.dominio.Notificacao;
import br.com.vitrinebauru.notificacoes.infraestrutura.envio.EnviadorDeEmail;
import br.com.vitrinebauru.notificacoes.infraestrutura.persistencia.NotificacaoRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Tira da fila e manda, com nova tentativa quando falha.
 *
 * <p>Mesma forma do publicador do outbox, e pela mesma razao: a entrega
 * depende de um servico de terceiro, e falha de terceiro nao pode virar evento
 * reprocessado nem cadastro sem aviso. Aqui a espera entre tentativas comeca
 * em um minuto e vai ate duas horas, porque e-mail atrasado alguns minutos nao
 * incomoda ninguem e provedor fora do ar costuma demorar mais que segundos
 * para voltar.
 */
@Component
public class EnviarPendentes {

    private static final Logger log = LoggerFactory.getLogger(EnviarPendentes.class);
    private static final int POR_RODADA = 20;

    private final NotificacaoRepository notificacoes;
    private final EnviadorDeEmail enviador;
    private final MeterRegistry metricas;
    private final Clock relogio;

    public EnviarPendentes(NotificacaoRepository notificacoes, EnviadorDeEmail enviador,
                           MeterRegistry metricas, Clock relogio) {
        this.notificacoes = notificacoes;
        this.enviador = enviador;
        this.metricas = metricas;
        this.relogio = relogio;
    }

    @Scheduled(fixedDelayString = "${vitrine.email.intervalo-ms:5000}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enviar() {
        var agora = relogio.instant();
        var pendentes = notificacoes.proximasParaEnviar(
                agora, Notificacao.TENTATIVAS_MAXIMAS, Limit.of(POR_RODADA));

        for (Notificacao notificacao : pendentes) {
            try {
                enviador.enviar(notificacao);
                notificacao.marcarEnviada(relogio.instant());
                metricas.counter("vitrine.email.enviados", "tipo", notificacao.tipo().name()).increment();

            } catch (Exception e) {
                notificacao.marcarFalha(e.getMessage(), relogio.instant());
                metricas.counter("vitrine.email.falhas", "tipo", notificacao.tipo().name()).increment();

                if (notificacao.esgotouTentativas()) {
                    log.error("E-mail {} para o empreendedor {} esgotou as tentativas. "
                                    + "A pessoa nao foi avisada e alguem precisa olhar.",
                            notificacao.tipo(), notificacao.empreendedorId(), e);
                } else {
                    log.warn("Falha ao enviar {} ({}a tentativa): {}",
                            notificacao.tipo(), notificacao.tentativas(), e.getMessage());
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${vitrine.email.intervalo-da-metrica-ms:30000}")
    public void medirFila() {
        metricas.gauge("vitrine.email.pendentes", notificacoes.countByEnviadaEmIsNull());
    }
}
