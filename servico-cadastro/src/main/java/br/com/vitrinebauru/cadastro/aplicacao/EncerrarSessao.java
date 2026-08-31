package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.SessaoDeRenovacaoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Sair da conta.
 *
 * <p>Sair e revogar a renovacao. O token de acesso continua tecnicamente
 * valido ate expirar, e isso e assumido: e o preco de nao consultar o banco a
 * cada requisicao, e por isso a validade dele e de quinze minutos.
 *
 * <p>Sair de token que nao existe nao da erro. Quem clicou em sair quer estar
 * fora, e mostrar "não foi possível sair" para alguem que ja esta fora so
 * assusta.
 */
@Component
public class EncerrarSessao {

    private final SessaoDeRenovacaoRepository sessoesSalvas;
    private final Sessoes sessoes;
    private final Auditor auditor;
    private final Clock relogio;

    public EncerrarSessao(SessaoDeRenovacaoRepository sessoesSalvas, Sessoes sessoes,
                          Auditor auditor, Clock relogio) {
        this.sessoesSalvas = sessoesSalvas;
        this.sessoes = sessoes;
        this.auditor = auditor;
        this.relogio = relogio;
    }

    @Transactional
    public void executar(String tokenDeRenovacao) {
        if (tokenDeRenovacao == null || tokenDeRenovacao.isBlank()) {
            return;
        }
        sessoesSalvas.findByHashDoToken(sessoes.resumo(tokenDeRenovacao)).ifPresent(sessao -> {
            sessao.revogar(relogio.instant());
            auditor.registrar(sessao.usuarioId(), "logout", "sessao", sessao.id(), null);
        });
    }

    /** Usado quando o proprio dono pede para derrubar tudo, e na exclusao de dados. */
    @Transactional
    public int todasDoUsuario(UUID usuarioId) {
        int derrubadas = sessoesSalvas.revogarTodasDoUsuario(usuarioId, relogio.instant());
        auditor.registrar(usuarioId, "logout_de_todas_as_sessoes", "usuario", usuarioId,
                derrubadas + " sessões encerradas");
        return derrubadas;
    }
}
