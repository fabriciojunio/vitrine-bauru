package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.dominio.PedidoDeExclusao;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.PedidoDeExclusaoRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository;
import br.com.vitrinebauru.contratos.ExclusaoConcluida;
import br.com.vitrinebauru.contratos.ExclusaoSolicitada;
import br.com.vitrinebauru.contratos.Participante;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.outbox.RegistroDeSaida;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/**
 * O coordenador da saga de exclusao.
 *
 * <p>Duas entradas: a confirmacao que chega de cada servico e o relogio. A
 * primeira faz a saga avancar; o segundo existe porque confirmacao que nunca
 * chega tambem e um final possivel, e o unico jeito de perceber isso e alguem
 * olhando o tempo passar.
 *
 * <p>Reenviar o pedido para quem nao respondeu e seguro porque apagar dado que
 * ja foi apagado nao faz nada. Essa e a propriedade que permite o coordenador
 * insistir sem medo em vez de esperar para sempre.
 */
@Component
public class ConduzirExclusao {

    private static final Logger log = LoggerFactory.getLogger(ConduzirExclusao.class);

    /** Depois disso, quem nao respondeu recebe o pedido de novo. */
    private static final Duration ESPERA_ANTES_DE_INSISTIR = Duration.ofMinutes(10);

    private final PedidoDeExclusaoRepository pedidos;
    private final EmpreendedorRepository empreendedores;
    private final UsuarioRepository usuarios;
    private final RegistroDeSaida outbox;
    private final Auditor auditor;
    private final MeterRegistry metricas;
    private final Clock relogio;

    public ConduzirExclusao(PedidoDeExclusaoRepository pedidos, EmpreendedorRepository empreendedores,
                            UsuarioRepository usuarios, RegistroDeSaida outbox, Auditor auditor,
                            MeterRegistry metricas, Clock relogio) {
        this.pedidos = pedidos;
        this.empreendedores = empreendedores;
        this.usuarios = usuarios;
        this.outbox = outbox;
        this.auditor = auditor;
        this.metricas = metricas;
        this.relogio = relogio;
    }

    /** Chamado pelo consumidor de eventos, ja dentro de transacao. */
    public void registrarConfirmacao(UUID empreendedorId, Participante participante, int removidos) {
        var pedido = pedidos.findByEmpreendedorId(empreendedorId).orElse(null);
        if (pedido == null) {
            log.warn("Confirmacao de expurgo de {} sem pedido correspondente. Ignorando.", participante);
            return;
        }

        boolean fechou = pedido.confirmar(participante);
        metricas.counter("vitrine.exclusao.confirmacoes", "participante", participante.name()).increment();
        log.info("{} confirmou expurgo de {} registros. Faltam: {}",
                participante, removidos, pedido.faltando());

        if (fechou) {
            concluir(pedido);
        }
    }

    private void concluir(PedidoDeExclusao pedido) {
        var agora = relogio.instant();

        empreendedores.findById(pedido.empreendedorId())
                .ifPresent(empreendedor -> empreendedor.anonimizar(agora));
        usuarios.findById(pedido.usuarioId())
                .ifPresent(usuario -> usuario.anonimizar(agora));

        pedido.concluir(agora);

        outbox.gravar(Topicos.PRIVACIDADE, new ExclusaoConcluida(
                UUID.randomUUID(), pedido.id(), agora, pedido.empreendedorId(), pedido.usuarioId()));

        auditor.registrar(null, "exclusao_concluida", "empreendedor", pedido.empreendedorId(),
                "Todos os serviços confirmaram o expurgo");
        metricas.counter("vitrine.exclusao.concluidas").increment();

        log.info("Exclusao do empreendedor {} concluida", pedido.empreendedorId());
    }

    /**
     * Varredura das sagas paradas.
     *
     * <p>Roda de dez em dez minutos. Nao e o caminho normal: no caminho normal
     * os tres servicos respondem em segundos. Isto existe para o dia em que um
     * deles ficou fora do ar durante o pedido, que e justamente o dia em que
     * ninguem vai lembrar de conferir na mao.
     */
    @Scheduled(fixedDelayString = "${vitrine.exclusao.intervalo-da-varredura-ms:600000}")
    @Transactional
    public void insistirNasParadas() {
        var agora = relogio.instant();
        var limite = agora.minus(ESPERA_ANTES_DE_INSISTIR);

        for (PedidoDeExclusao pedido : pedidos.emAndamento()) {
            boolean esperouTempoDemais = pedido.ultimoLembreteEm() == null
                    ? pedido.solicitadoEm().isBefore(limite)
                    : pedido.ultimoLembreteEm().isBefore(limite);

            if (!esperouTempoDemais) {
                continue;
            }

            log.warn("Exclusao do empreendedor {} parada ha tempo demais. Faltam {}. Reenviando.",
                    pedido.empreendedorId(), pedido.faltando());

            outbox.gravar(Topicos.PRIVACIDADE, new ExclusaoSolicitada(
                    UUID.randomUUID(), pedido.id(), agora,
                    pedido.empreendedorId(), pedido.usuarioId(), pedido.prazoLimite()));
            pedido.anotarLembrete(agora);
            metricas.counter("vitrine.exclusao.reenvios").increment();

            if (pedido.estaAtrasado(agora)) {
                log.error("PRAZO LEGAL ESTOURADO: exclusao do empreendedor {} passou de {}. "
                                + "Servicos que nao responderam: {}. Precisa de acao manual.",
                        pedido.empreendedorId(), pedido.prazoLimite(), pedido.faltando());
                metricas.counter("vitrine.exclusao.atrasadas").increment();
                auditor.registrar(null, "exclusao_atrasada", "empreendedor", pedido.empreendedorId(),
                        "Faltam: " + pedido.faltando());
            }
        }
    }

    /** Vira metrica para o alerta existir antes de alguem reclamar. */
    @Scheduled(fixedDelayString = "${vitrine.exclusao.intervalo-da-metrica-ms:60000}")
    public void medirPendentes() {
        metricas.gauge("vitrine.exclusao.em_andamento", pedidos.emAndamento().size());
        metricas.gauge("vitrine.exclusao.fora_do_prazo", pedidos.atrasados(relogio.instant()).size());
    }
}
