package br.com.vitrinebauru.plataforma.seguranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Emissor de token")
class EmissorDeTokenTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-trinta-e-dois-bytes";
    private static final Instant AGORA = Instant.parse("2026-09-22T12:00:00Z");

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID empreendedorId = UUID.randomUUID();

    private EmissorDeToken emissorEm(Instant momento, String segredo) {
        var propriedades = new PropriedadesDeSeguranca(
                segredo, Duration.ofMinutes(15), Duration.ofDays(7), List.of("http://localhost:5173"));
        return new EmissorDeToken(propriedades, Clock.fixed(momento, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("devolve no token o mesmo usuario que entrou")
    void idaEVolta() {
        var emissor = emissorEm(AGORA, SEGREDO);
        var usuario = new UsuarioAutenticado(usuarioId, "lourdes@exemplo.com",
                Papel.EMPREENDEDOR, empreendedorId);

        var lido = emissor.ler(emissor.emitir(usuario));

        assertThat(lido).contains(usuario);
    }

    @Test
    @DisplayName("aceita administrador sem empreendedor vinculado")
    void administradorSemEmpreendedor() {
        var emissor = emissorEm(AGORA, SEGREDO);
        var admin = new UsuarioAutenticado(usuarioId, "sedecon@bauru.sp.gov.br",
                Papel.ADMIN_SEDECON, null);

        var lido = emissor.ler(emissor.emitir(admin));

        assertThat(lido).isPresent();
        assertThat(lido.get().empreendedorId()).isNull();
        assertThat(lido.get().ehAdministrador()).isTrue();
    }

    @Test
    @DisplayName("recusa token expirado")
    void recusaExpirado() {
        var token = emissorEm(AGORA, SEGREDO).emitir(
                new UsuarioAutenticado(usuarioId, "lourdes@exemplo.com", Papel.EMPREENDEDOR, empreendedorId));

        var depois = emissorEm(AGORA.plus(Duration.ofMinutes(16)), SEGREDO);

        assertThat(depois.ler(token)).isEmpty();
    }

    @Test
    @DisplayName("aceita token dentro da validade")
    void aceitaDentroDaValidade() {
        var token = emissorEm(AGORA, SEGREDO).emitir(
                new UsuarioAutenticado(usuarioId, "lourdes@exemplo.com", Papel.EMPREENDEDOR, empreendedorId));

        var quaseLa = emissorEm(AGORA.plus(Duration.ofMinutes(14)), SEGREDO);

        assertThat(quaseLa.ler(token)).isPresent();
    }

    @Test
    @DisplayName("recusa token assinado com outro segredo")
    void recusaOutroSegredo() {
        var token = emissorEm(AGORA, SEGREDO).emitir(
                new UsuarioAutenticado(usuarioId, "lourdes@exemplo.com", Papel.EMPREENDEDOR, empreendedorId));

        var outro = emissorEm(AGORA, "outro-segredo-de-teste-com-trinta-e-dois-bytes");

        assertThat(outro.ler(token)).isEmpty();
    }

    @Test
    @DisplayName("recusa token adulterado no meio")
    void recusaAdulterado() {
        var emissor = emissorEm(AGORA, SEGREDO);
        String token = emissor.emitir(
                new UsuarioAutenticado(usuarioId, "lourdes@exemplo.com", Papel.EMPREENDEDOR, empreendedorId));

        String[] partes = token.split("\\.");
        String adulterado = partes[0] + "." + partes[1].substring(0, partes[1].length() - 2) + "AA."
                + partes[2];

        assertThat(emissor.ler(adulterado)).isEmpty();
    }

    @Test
    @DisplayName("recusa texto que nem token e")
    void recusaLixo() {
        var emissor = emissorEm(AGORA, SEGREDO);

        assertThat(emissor.ler("")).isEmpty();
        assertThat(emissor.ler("nao-e-token")).isEmpty();
        assertThat(emissor.ler("a.b.c")).isEmpty();
    }

    @Test
    @DisplayName("nao aceita segredo curto demais para HMAC")
    void recusaSegredoCurto() {
        assertThatThrownBy(() -> new PropriedadesDeSeguranca(
                "curto", Duration.ofMinutes(15), Duration.ofDays(7), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("nao aceita token de acesso com validade longa")
    void recusaValidadeLonga() {
        assertThatThrownBy(() -> new PropriedadesDeSeguranca(
                SEGREDO, Duration.ofHours(8), Duration.ofDays(7), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("uma hora");
    }

    @Test
    @DisplayName("aplica padrao quando duracao e origem nao foram configuradas")
    void aplicaPadroes() {
        var propriedades = new PropriedadesDeSeguranca(SEGREDO, null, null, null);

        assertThat(propriedades.duracaoDoAcesso()).isEqualTo(Duration.ofMinutes(15));
        assertThat(propriedades.duracaoDoRefresh()).isEqualTo(Duration.ofDays(7));
        assertThat(propriedades.origensPermitidas()).containsExactly("http://localhost:5173");
    }
}
