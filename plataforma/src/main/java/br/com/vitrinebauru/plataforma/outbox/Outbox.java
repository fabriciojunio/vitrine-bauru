package br.com.vitrinebauru.plataforma.outbox;

import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.plataforma.mensageria.MapeadorDeEventos;
import br.com.vitrinebauru.plataforma.observabilidade.RastroDaMensagem;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * Grava evento na mesma transação que mudou o estado.
 *
 * <p>Este é o ponto inteiro do padrão: aprovar um cadastro e avisar que um
 * cadastro foi aprovado precisam acontecer juntos ou não acontecer. Escrever
 * no banco e depois chamar o broker resolveria 99% das vezes, e o 1% restante
 * seria um empreendedor aprovado no banco cuja loja nunca apareceu na busca,
 * sem ninguém saber por que.
 *
 * <p>Chamar isto fora de uma transação compila e roda, e é o único jeito de
 * errar com outbox. Por isso existe o teste de arquitetura que exige
 * {@code @Transactional} em quem grava.
 */
@Component
public class Outbox implements RegistroDeSaida {

    private final OutboxRepository repositorio;
    private final MapeadorDeEventos mapeador;
    private final Clock relogio;
    private final RastroDaMensagem rastro;

    public Outbox(OutboxRepository repositorio, MapeadorDeEventos mapeador, Clock relogio,
                  RastroDaMensagem rastro) {
        this.repositorio = repositorio;
        this.mapeador = mapeador;
        this.relogio = relogio;
        this.rastro = rastro;
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
                relogio.instant(),
                // Capturado aqui, e não na publicação: aqui ainda existe o
                // contexto da requisição que originou o evento. Na publicação
                // ela já terminou.
                rastro.capturar());
    }
}
