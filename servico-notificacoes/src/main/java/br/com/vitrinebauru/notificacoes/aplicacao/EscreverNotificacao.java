package br.com.vitrinebauru.notificacoes.aplicacao;

import br.com.vitrinebauru.notificacoes.dominio.Mensagens;
import br.com.vitrinebauru.notificacoes.dominio.Notificacao;
import br.com.vitrinebauru.notificacoes.dominio.TipoDeNotificacao;
import br.com.vitrinebauru.notificacoes.infraestrutura.persistencia.NotificacaoRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * Transforma evento em e-mail a enviar.
 *
 * <p>O identificador da notificação é o id do próprio evento que a originou.
 * Com isso, a mesma mensagem reentregue pelo broker esbarra na chave primária
 * em vez de gerar um segundo e-mail: o empreendedor não recebe duas vezes que
 * o cadastro dele foi aprovado, e a idempotência não depende só do inbox.
 */
@Component
public class EscreverNotificacao {

    private final NotificacaoRepository notificacoes;
    private final Clock relogio;

    public EscreverNotificacao(NotificacaoRepository notificacoes, Clock relogio) {
        this.notificacoes = notificacoes;
        this.relogio = relogio;
    }

    public void boasVindas(UUID eventoId, UUID empreendedorId, String email,
                           String nome, String nomeDoNegocio) {
        gravar(eventoId, empreendedorId, email, TipoDeNotificacao.BOAS_VINDAS,
                Mensagens.boasVindas(nome, nomeDoNegocio));
    }

    public void aprovado(UUID eventoId, UUID empreendedorId, String email,
                         String nome, String nomeDoNegocio) {
        gravar(eventoId, empreendedorId, email, TipoDeNotificacao.CADASTRO_APROVADO,
                Mensagens.aprovado(nome, nomeDoNegocio));
    }

    public void rejeitado(UUID eventoId, UUID empreendedorId, String email,
                          String nome, String nomeDoNegocio, String motivo) {
        gravar(eventoId, empreendedorId, email, TipoDeNotificacao.CADASTRO_REJEITADO,
                Mensagens.rejeitado(nome, nomeDoNegocio, motivo));
    }

    public void suspenso(UUID eventoId, UUID empreendedorId, String email,
                         String nome, String nomeDoNegocio, String motivo) {
        gravar(eventoId, empreendedorId, email, TipoDeNotificacao.LOJA_SUSPENSA,
                Mensagens.suspenso(nome, nomeDoNegocio, motivo));
    }

    private void gravar(UUID eventoId, UUID empreendedorId, String email,
                        TipoDeNotificacao tipo, Mensagens.Conteudo conteudo) {
        if (notificacoes.existsById(eventoId)) {
            return;
        }
        notificacoes.save(Notificacao.nova(eventoId, empreendedorId, email, tipo,
                conteudo.assunto(), conteudo.corpo(), relogio.instant()));
    }
}
