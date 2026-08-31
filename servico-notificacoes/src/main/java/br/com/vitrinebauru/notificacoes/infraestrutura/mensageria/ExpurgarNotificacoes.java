package br.com.vitrinebauru.notificacoes.infraestrutura.mensageria;

import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.contratos.ExclusaoSolicitada;
import br.com.vitrinebauru.contratos.ExpurgoConcluido;
import br.com.vitrinebauru.contratos.Participante;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.notificacoes.infraestrutura.persistencia.NotificacaoRepository;
import br.com.vitrinebauru.plataforma.mensageria.ConsumidorDeEventos;
import br.com.vitrinebauru.plataforma.outbox.RegistroDeSaida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

/**
 * A parte das notificacoes no pedido de exclusao.
 *
 * <p>O historico de e-mail guarda nome, endereco de e-mail e o motivo escrito
 * pela analise. E o servico com menos codigo dos tres participantes e o que
 * mais guarda texto sobre a pessoa, e por isso ele entra na saga.
 */
@Component
public class ExpurgarNotificacoes implements ConsumidorDeEventos {

    private static final Logger log = LoggerFactory.getLogger(ExpurgarNotificacoes.class);

    private final NotificacaoRepository notificacoes;
    private final RegistroDeSaida outbox;
    private final Clock relogio;

    public ExpurgarNotificacoes(NotificacaoRepository notificacoes, RegistroDeSaida outbox,
                                Clock relogio) {
        this.notificacoes = notificacoes;
        this.outbox = outbox;
        this.relogio = relogio;
    }

    @Override
    public String nome() {
        return "notificacoes-expurgo";
    }

    @Override
    public Set<String> topicos() {
        return Set.of(Topicos.PRIVACIDADE);
    }

    @Override
    public void consumir(Evento evento) {
        if (!(evento instanceof ExclusaoSolicitada pedido)) {
            return;
        }

        int removidas = notificacoes.deleteByEmpreendedorId(pedido.empreendedorId());
        log.info("Expurgo de notificacoes do empreendedor {}: {} mensagens apagadas",
                pedido.empreendedorId(), removidas);

        outbox.gravar(Topicos.PRIVACIDADE, new ExpurgoConcluido(
                UUID.randomUUID(), pedido.correlacao(), relogio.instant(),
                pedido.empreendedorId(), Participante.NOTIFICACOES, removidas));
    }
}
