package br.com.vitrinebauru.notificacoes.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notificação")
class NotificacaoTest {

    private static final Instant AGORA = Instant.parse("2026-09-22T12:00:00Z");

    private Notificacao nova() {
        return Notificacao.nova(UUID.randomUUID(), UUID.randomUUID(), "lourdes@exemplo.com",
                TipoDeNotificacao.CADASTRO_APROVADO, "Sua loja está no ar", "Olá, Lourdes.", AGORA);
    }

    @Test
    @DisplayName("nasce na fila, sem tentativa")
    void nasceNaFila() {
        var notificacao = nova();

        assertThat(notificacao.foiEnviada()).isFalse();
        assertThat(notificacao.tentativas()).isZero();
        assertThat(notificacao.esgotouTentativas()).isFalse();
    }

    @Test
    @DisplayName("enviada limpa o erro anterior")
    void enviadaLimpaErro() {
        var notificacao = nova();
        notificacao.marcarFalha("provedor fora do ar", AGORA);

        notificacao.marcarEnviada(AGORA.plusSeconds(120));

        assertThat(notificacao.foiEnviada()).isTrue();
        assertThat(notificacao.ultimoErro()).isNull();
        assertThat(notificacao.proximaTentativa()).isNull();
    }

    @Test
    @DisplayName("espera dobrada a cada falha, começando em um minuto")
    void esperaCrescente() {
        var notificacao = nova();

        notificacao.marcarFalha("erro", AGORA);
        assertThat(notificacao.proximaTentativa()).isEqualTo(AGORA.plus(Duration.ofMinutes(1)));

        notificacao.marcarFalha("erro", AGORA);
        assertThat(notificacao.proximaTentativa()).isEqualTo(AGORA.plus(Duration.ofMinutes(2)));

        notificacao.marcarFalha("erro", AGORA);
        assertThat(notificacao.proximaTentativa()).isEqualTo(AGORA.plus(Duration.ofMinutes(4)));
    }

    @Test
    @DisplayName("a espera para de crescer em duas horas")
    void esperaTemTeto() {
        var notificacao = nova();

        for (int tentativa = 0; tentativa < 10; tentativa++) {
            notificacao.marcarFalha("erro", AGORA);
        }

        assertThat(notificacao.proximaTentativa()).isEqualTo(AGORA.plus(Duration.ofHours(2)));
    }

    @Test
    @DisplayName("desiste depois de cinco tentativas, para alguém olhar")
    void desisteDepoisDeCinco() {
        var notificacao = nova();

        for (int tentativa = 0; tentativa < Notificacao.TENTATIVAS_MAXIMAS; tentativa++) {
            assertThat(notificacao.esgotouTentativas()).isFalse();
            notificacao.marcarFalha("erro", AGORA);
        }

        assertThat(notificacao.esgotouTentativas()).isTrue();
    }

    @Test
    @DisplayName("corta erro comprido para não estourar a coluna")
    void cortaErroComprido() {
        var notificacao = nova();

        notificacao.marcarFalha("x".repeat(900), AGORA);

        assertThat(notificacao.ultimoErro()).hasSize(400);
    }

    @Test
    @DisplayName("aceita falha sem mensagem")
    void aceitaFalhaSemMensagem() {
        var notificacao = nova();

        notificacao.marcarFalha(null, AGORA);

        assertThat(notificacao.ultimoErro()).isEqualTo("sem detalhe");
    }
}
