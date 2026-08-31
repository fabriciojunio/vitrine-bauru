package br.com.vitrinebauru.plataforma.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mensagem do outbox")
class MensagemDoOutboxTest {

    private static final Instant AGORA = Instant.parse("2026-09-22T12:00:00Z");

    private MensagemDoOutbox nova() {
        return MensagemDoOutbox.nova(UUID.randomUUID(), "vitrine.empreendedores", "chave-1",
                "CadastroAprovado", "{}", AGORA);
    }

    @Test
    @DisplayName("nasce pendente, sem tentativa")
    void nasceNdente() {
        var mensagem = nova();

        assertThat(mensagem.foiPublicada()).isFalse();
        assertThat(mensagem.tentativas()).isZero();
        assertThat(mensagem.esgotouTentativas()).isFalse();
        assertThat(mensagem.proximaTentativa()).isNull();
    }

    @Test
    @DisplayName("publicada limpa o erro anterior")
    void publicadaLimpaErro() {
        var mensagem = nova();
        mensagem.marcarFalha("broker fora do ar", AGORA);

        mensagem.marcarPublicada(AGORA.plusSeconds(5));

        assertThat(mensagem.foiPublicada()).isTrue();
        assertThat(mensagem.ultimoErro()).isNull();
        assertThat(mensagem.proximaTentativa()).isNull();
    }

    @Test
    @DisplayName("dobra a espera a cada falha, até dez minutos")
    void esperaCrescente() {
        var mensagem = nova();

        mensagem.marcarFalha("erro", AGORA);
        assertThat(mensagem.proximaTentativa()).isEqualTo(AGORA.plus(Duration.ofSeconds(2)));

        mensagem.marcarFalha("erro", AGORA);
        assertThat(mensagem.proximaTentativa()).isEqualTo(AGORA.plus(Duration.ofSeconds(4)));

        mensagem.marcarFalha("erro", AGORA);
        assertThat(mensagem.proximaTentativa()).isEqualTo(AGORA.plus(Duration.ofSeconds(8)));

        mensagem.marcarFalha("erro", AGORA);
        assertThat(mensagem.proximaTentativa()).isEqualTo(AGORA.plus(Duration.ofSeconds(16)));
    }

    @Test
    @DisplayName("a espera não passa de dez minutos, senao a fila nunca mais anda")
    void esperaTemTeto() {
        var mensagem = nova();

        for (int tentativa = 0; tentativa < 12; tentativa++) {
            mensagem.marcarFalha("erro", AGORA);
        }

        assertThat(mensagem.proximaTentativa()).isEqualTo(AGORA.plus(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("para de tentar depois do limite e fica esperando gente")
    void paraDepoisDoLimite() {
        var mensagem = nova();

        for (int tentativa = 0; tentativa < MensagemDoOutbox.TENTATIVAS_MAXIMAS; tentativa++) {
            assertThat(mensagem.esgotouTentativas()).isFalse();
            mensagem.marcarFalha("erro", AGORA);
        }

        assertThat(mensagem.esgotouTentativas()).isTrue();
        assertThat(mensagem.tentativas()).isEqualTo(MensagemDoOutbox.TENTATIVAS_MAXIMAS);
    }

    @Test
    @DisplayName("corta erro comprido para não estourar a coluna")
    void cortaErroComprido() {
        var mensagem = nova();

        mensagem.marcarFalha("x".repeat(900), AGORA);

        assertThat(mensagem.ultimoErro()).hasSize(500);
    }

    @Test
    @DisplayName("aceita falha sem mensagem, que e o caso do NullPointerException")
    void aceitaFalhaSemMensagem() {
        var mensagem = nova();

        mensagem.marcarFalha(null, AGORA);

        assertThat(mensagem.ultimoErro()).isEqualTo("sem detalhe");
    }

    @Test
    @DisplayName("guarda os dados da mensagem sem alterar")
    void guardaDados() {
        UUID id = UUID.randomUUID();
        var mensagem = MensagemDoOutbox.nova(id, "vitrine.catalogo", "loja-1",
                "ProdutoPublicado", "{\"nome\":\"Bolo\"}", AGORA);

        assertThat(mensagem.id()).isEqualTo(id);
        assertThat(mensagem.topico()).isEqualTo("vitrine.catalogo");
        assertThat(mensagem.chave()).isEqualTo("loja-1");
        assertThat(mensagem.tipo()).isEqualTo("ProdutoPublicado");
        assertThat(mensagem.carga()).contains("Bolo");
        assertThat(mensagem.criadaEm()).isEqualTo(AGORA);
    }
}
