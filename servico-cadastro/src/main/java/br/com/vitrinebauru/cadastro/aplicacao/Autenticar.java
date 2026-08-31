package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.dominio.Usuario;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Login por e-mail e senha.
 *
 * <h2>A mesma resposta para e-mail errado e senha errada</h2>
 * Responder "esse e-mail nao existe" transforma a tela de login numa lista de
 * quem tem conta na plataforma. E como o cadastro aqui e de pequeno
 * empreendedor com nome e telefone publicos, essa lista tem valor para quem
 * aplica golpe.
 *
 * <h2>Conferir senha mesmo quando o usuario nao existe</h2>
 * Bcrypt e lento de proposito. Se a resposta para e-mail inexistente voltasse
 * na hora e a de senha errada demorasse, o tempo de resposta entregaria o que
 * a mensagem esconde. Por isso o codigo confere a senha contra um hash de
 * mentira nesse caso.
 */
@Component
public class Autenticar {

    private static final Logger log = LoggerFactory.getLogger(Autenticar.class);

    /**
     * Hash de bcrypt valido, de uma senha que nao e de ninguem. Serve so para
     * gastar o mesmo tempo do caminho normal.
     */
    private static final String HASH_DE_MENTIRA =
            "$2a$12$k9y0lPqI7VHXQ5hJZ3n0auRLqvY4XxKe0kX0mVJ8b6oZ4b3lZ1qDe";

    private static final String MENSAGEM_GENERICA = "E-mail ou senha incorretos.";

    private final UsuarioRepository usuarios;
    private final EmpreendedorRepository empreendedores;
    private final PasswordEncoder codificador;
    private final Sessoes sessoes;
    private final Auditor auditor;
    private final RegistroDeSeguranca registroDeSeguranca;
    private final Clock relogio;

    public Autenticar(UsuarioRepository usuarios, EmpreendedorRepository empreendedores,
                      PasswordEncoder codificador, Sessoes sessoes, Auditor auditor,
                      RegistroDeSeguranca registroDeSeguranca, Clock relogio) {
        this.usuarios = usuarios;
        this.empreendedores = empreendedores;
        this.codificador = codificador;
        this.sessoes = sessoes;
        this.auditor = auditor;
        this.registroDeSeguranca = registroDeSeguranca;
        this.relogio = relogio;
    }

    @Transactional
    public Sessoes.Aberta executar(String email, String senha) {
        Optional<Usuario> encontrado = usuarios.findByEmail(Usuario.normalizarEmail(email));

        if (encontrado.isEmpty()) {
            codificador.matches(senha == null ? "" : senha, HASH_DE_MENTIRA);
            log.info("Tentativa de login com e-mail nao cadastrado");
            throw new ErrosDeNegocio.NaoAutenticado(MENSAGEM_GENERICA);
        }

        Usuario usuario = encontrado.get();
        var agora = relogio.instant();

        if (usuario.estaBloqueado(agora)) {
            long minutos = Math.max(1, Duration.between(agora, usuario.bloqueadoAte()).toMinutes());
            throw new ErrosDeNegocio.Proibido(
                    "Conta bloqueada por tentativas seguidas. Tente de novo em " + minutos
                            + (minutos == 1 ? " minuto." : " minutos."));
        }

        usuario.exigirAtiva();

        if (!codificador.matches(senha == null ? "" : senha, usuario.senhaHash())) {
            // Transacao propria: esta aqui vai ser desfeita pela excecao logo
            // abaixo, e com ela o contador de tentativas voltaria a zero.
            registroDeSeguranca.anotarSenhaErrada(usuario.id());
            throw new ErrosDeNegocio.NaoAutenticado(MENSAGEM_GENERICA);
        }

        usuario.registrarAcertoDeSenha(agora);
        auditor.registrar(usuario.id(), "login", "usuario", usuario.id(), null);

        UUID empreendedorId = empreendedores.findByUsuarioId(usuario.id())
                .map(empreendedor -> empreendedor.id())
                .orElse(null);

        return sessoes.abrir(usuario, empreendedorId);
    }
}
