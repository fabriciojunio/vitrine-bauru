package br.com.vitrinebauru.plataforma.outbox;

import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.plataforma.mensageria.MapeadorDeEventos;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * Grava evento na mesma transacao que mudou o estado.
 *
 * <p>Este e o ponto inteiro do padrao: aprovar um cadastro e avisar que um
 * cadastro foi aprovado precisam acontecer juntos ou nao acontecer. Escrever
 * no banco e depois chamar o broker resolveria 99% das vezes, e o 1% restante
 * seria um empreendedor aprovado no banco cuja loja nunca apareceu na busca,
 * sem ninguem saber por que.
 *
 * <p>Chamar isto fora de uma transacao compila e roda, e e o unico jeito de
 * errar com outbox. Por isso existe o teste de arquitetura que exige
 * {@code @Transactional} em quem grava.
 */
@Component
public class Outbox implements RegistroDeSaida {

    private final OutboxRepository repositorio;
    private final MapeadorDeEventos mapeador;
    private final Clock relogio;

    public Outbox(OutboxRepository repositorio, MapeadorDeEventos mapeador, Clock relogio) {
        this.repositorio = repositorio;
        this.mapeador = mapeador;
        this.relogio = relogio;
    }

    @Override
    public void gravar(String topico, Evento evento) {
        repositorio.save(paraMensagem(topico, evento));
    }

    public void gravarTodos(String topico, List<? extends Evento> eventos) {
        if (eventos.isEmpty()) {
            return;
        }
        repositorio.saveAll(eventos.stream().map(evento -> paraMensagem(topico, evento)).toList());
    }

    private MensagemDoOutbox paraMensagem(String topico, Evento evento) {
        return MensagemDoOutbox.nova(
                evento.id(),
                topico,
                evento.chaveDeParticao().toString(),
                evento.tipoDoEvento(),
                mapeador.paraJson(evento),
                relogio.instant());
    }
}
