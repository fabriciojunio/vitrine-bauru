package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.dominio.RegistroDeAuditoria;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.AuditoriaRepository;
import br.com.vitrinebauru.plataforma.web.Correlacao;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * Grava o rastro de quem fez o que.
 *
 * <p>Sempre chamado de dentro da transacao que executou a acao, e nunca
 * depois. Auditoria escrita em transacao separada e auditoria que registra
 * coisa que nao aconteceu (quando a acao falha em seguida) ou que perde coisa
 * que aconteceu (quando o registro falha sozinho).
 */
@Component
public class Auditor {

    private final AuditoriaRepository repositorio;
    private final Clock relogio;

    public Auditor(AuditoriaRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    public void registrar(UUID autor, String acao, String entidade, UUID entidadeId, String detalhe) {
        repositorio.save(RegistroDeAuditoria.de(
                autor, acao, entidade, entidadeId, detalhe, Correlacao.atual(), relogio.instant()));
    }
}
