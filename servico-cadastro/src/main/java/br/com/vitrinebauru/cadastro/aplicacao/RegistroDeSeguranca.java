package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.SessaoDeRenovacaoRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * O que precisa ficar gravado mesmo quando a requisição termina em erro.
 *
 * <h2>Por que isto existe</h2>
 * Contar senha errada e revogar sessão roubada acontecem no caminho que
 * termina lançando exceção. E exceção desfaz a transação: o contador subia,
 * a exceção subia junto, o banco voltava atrás e o contador nunca passava de
 * zero. O bloqueio por tentativa existia no código e não existia na prática.
 *
 * <p>Uma transação própria, que fecha antes de a de fora ser desfeita, é o que
 * resolve. Foi um teste de integração que mostrou isso; nenhum teste de
 * unidade com repositório de mentira pegaria, porque em memória não existe
 * transação para desfazer.
 */
@Component
public class RegistroDeSeguranca {

    private final UsuarioRepository usuarios;
    private final SessaoDeRenovacaoRepository sessoes;
    private final Auditor auditor;
    private final Clock relogio;

    public RegistroDeSeguranca(UsuarioRepository usuarios, SessaoDeRenovacaoRepository sessoes,
                               Auditor auditor, Clock relogio) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
        this.auditor = auditor;
        this.relogio = relogio;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void anotarSenhaErrada(UUID usuarioId) {
        usuarios.findById(usuarioId).ifPresent(usuario -> {
            usuario.registrarErroDeSenha(relogio.instant());
            usuarios.save(usuario);
            auditor.registrar(usuario.id(), "login_recusado", "usuario", usuario.id(),
                    "Tentativa " + usuario.tentativasFalhas());
        });
    }

    /**
     * Derruba todas as sessões do usuário. Chamado quando um token de
     * renovação já gasto reaparece, que só acontece por cópia roubada ou cópia
     * antiga em uso: nos dois casos, manter as sessões vivas favorece quem
     * não devia estar lá.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int derrubarTodasAsSessoes(UUID usuarioId, UUID sessaoSuspeita) {
        int derrubadas = sessoes.revogarTodasDoUsuario(usuarioId, relogio.instant());
        auditor.registrar(usuarioId, "reuso_de_token_detectado", "sessao", sessaoSuspeita,
                derrubadas + " sessões revogadas por segurança");
        return derrubadas;
    }
}
