package br.com.vitrinebauru.cadastro.dominio;

import br.com.vitrinebauru.plataforma.seguranca.Papel;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Usuário")
class UsuarioTest {

    private static final Instant AGORA = Instant.parse("2026-09-22T12:00:00Z");

    private Usuario novo() {
        return Usuario.novo("Maria de Lourdes", "Lourdes@Exemplo.COM", "hash", Papel.EMPREENDEDOR, AGORA);
    }

    @Test
    @DisplayName("guarda o e-mail em minusculas")
    void normalizaEmail() {
        assertThat(novo().email()).isEqualTo("lourdes@exemplo.com");
    }

    @ParameterizedTest(name = "\"{0}\" vira \"{1}\"")
    @CsvSource({
            "'Lourdes@Exemplo.com', 'lourdes@exemplo.com'",
            "'  espaco@exemplo.com  ', 'espaco@exemplo.com'",
            "'MAIUSCULA@EXEMPLO.COM', 'maiuscula@exemplo.com'"
    })
    void normalizaVariasFormas(String entrada, String esperado) {
        assertThat(Usuario.normalizarEmail(entrada)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("nasce ativa e sem bloqueio")
    void nasceAtiva() {
        var usuario = novo();

        assertThat(usuario.ativo()).isTrue();
        assertThat(usuario.tentativasFalhas()).isZero();
        assertThat(usuario.estaBloqueado(AGORA)).isFalse();
    }

    @Test
    @DisplayName("bloqueia depois de cinco erros seguidos")
    void bloqueiaDepoisDeCincoErros() {
        var usuario = novo();

        for (int tentativa = 1; tentativa < Usuario.TENTATIVAS_ATE_BLOQUEAR; tentativa++) {
            usuario.registrarErroDeSenha(AGORA);
            assertThat(usuario.estaBloqueado(AGORA)).isFalse();
        }
        usuario.registrarErroDeSenha(AGORA);

        assertThat(usuario.estaBloqueado(AGORA)).isTrue();
        assertThat(usuario.bloqueadoAte()).isEqualTo(AGORA.plus(Usuario.DURACAO_DO_BLOQUEIO));
    }

    @Test
    @DisplayName("o bloqueio passa sozinho depois do tempo")
    void bloqueioExpira() {
        var usuario = novo();
        for (int tentativa = 0; tentativa < Usuario.TENTATIVAS_ATE_BLOQUEAR; tentativa++) {
            usuario.registrarErroDeSenha(AGORA);
        }

        var depois = AGORA.plus(Usuario.DURACAO_DO_BLOQUEIO).plus(Duration.ofSeconds(1));

        assertThat(usuario.estaBloqueado(depois)).isFalse();
    }

    @Test
    @DisplayName("acertar a senha zera o contador e guarda o acesso")
    void acertoZeraContador() {
        var usuario = novo();
        usuario.registrarErroDeSenha(AGORA);
        usuario.registrarErroDeSenha(AGORA);

        usuario.registrarAcertoDeSenha(AGORA.plusSeconds(30));

        assertThat(usuario.tentativasFalhas()).isZero();
        assertThat(usuario.bloqueadoAte()).isNull();
        assertThat(usuario.ultimoAcessoEm()).isEqualTo(AGORA.plusSeconds(30));
    }

    @Test
    @DisplayName("trocar a senha destrava a conta")
    void trocaDeSenhaDestrava() {
        var usuario = novo();
        for (int tentativa = 0; tentativa < Usuario.TENTATIVAS_ATE_BLOQUEAR; tentativa++) {
            usuario.registrarErroDeSenha(AGORA);
        }

        usuario.trocarSenha("outro-hash");

        assertThat(usuario.estaBloqueado(AGORA)).isFalse();
        assertThat(usuario.senhaHash()).isEqualTo("outro-hash");
    }

    @Test
    @DisplayName("conta desativada nao entra")
    void contaDesativadaNaoEntra() {
        var usuario = novo();
        usuario.anonimizar(AGORA);

        assertThatThrownBy(usuario::exigirAtiva)
                .isInstanceOf(ErrosDeNegocio.Proibido.class)
                .hasMessageContaining("desativada");
    }

    @Test
    @DisplayName("anonimizar tira nome, e-mail e senha, e mantem o identificador")
    void anonimiza() {
        var usuario = novo();
        var id = usuario.id();

        usuario.anonimizar(AGORA);

        assertThat(usuario.id()).isEqualTo(id);
        assertThat(usuario.nome()).isEqualTo("Conta removida");
        assertThat(usuario.email()).doesNotContain("lourdes");
        assertThat(usuario.email()).endsWith("@vitrinebauru.invalido");
        assertThat(usuario.ativo()).isFalse();
        assertThat(usuario.anonimizadoEm()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("o e-mail anonimizado nao colide com o de outra conta removida")
    void emailAnonimizadoNaoColide() {
        var primeira = novo();
        var segunda = Usuario.novo("Outro", "outro@exemplo.com", "hash", Papel.EMPREENDEDOR, AGORA);

        primeira.anonimizar(AGORA);
        segunda.anonimizar(AGORA);

        assertThat(primeira.email()).isNotEqualTo(segunda.email());
    }

    @Test
    @DisplayName("o papel vira autoridade no formato do Spring Security")
    void papelViraAutoridade() {
        assertThat(Papel.ADMIN_SEDECON.comoAutoridade()).isEqualTo("ROLE_ADMIN_SEDECON");
        assertThat(Papel.EMPREENDEDOR.comoAutoridade()).isEqualTo("ROLE_EMPREENDEDOR");
    }
}
