package br.com.vitrinebauru.cadastro.dominio;

import br.com.vitrinebauru.contratos.Participante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Sessão de renovação e pedido de exclusão")
class SessaoEExclusaoTest {

    private static final Instant AGORA = Instant.parse("2026-09-22T12:00:00Z");

    @Nested
    @DisplayName("Sessão de renovação")
    class Sessao {

        private SessaoDeRenovacao nova() {
            return SessaoDeRenovacao.nova(UUID.randomUUID(), "hash-do-token", AGORA,
                    AGORA.plus(Duration.ofDays(7)));
        }

        @Test
        @DisplayName("nasce válida")
        void nasceValida() {
            var sessao = nova();

            assertThat(sessao.estaValida(AGORA)).isTrue();
            assertThat(sessao.jaFoiUsada()).isFalse();
        }

        @Test
        @DisplayName("deixa de valer depois de usada")
        void deixaDeValerDepoisDeUsada() {
            var sessao = nova();
            var proxima = UUID.randomUUID();

            sessao.usar(proxima, AGORA.plusSeconds(60));

            assertThat(sessao.estaValida(AGORA.plusSeconds(61))).isFalse();
            assertThat(sessao.jaFoiUsada()).isTrue();
            assertThat(sessao.substituidaPor()).isEqualTo(proxima);
        }

        @Test
        @DisplayName("deixa de valer depois de revogada")
        void deixaDeValerDepoisDeRevogada() {
            var sessao = nova();

            sessao.revogar(AGORA.plusSeconds(10));

            assertThat(sessao.estaValida(AGORA.plusSeconds(11))).isFalse();
        }

        @Test
        @DisplayName("revogar duas vezes mantém a primeira data")
        void revogarDuasVezes() {
            var sessao = nova();
            var primeira = AGORA.plusSeconds(10);

            sessao.revogar(primeira);
            sessao.revogar(AGORA.plusSeconds(300));

            assertThat(sessao.revogadaEm()).isEqualTo(primeira);
        }

        @Test
        @DisplayName("expira sozinha no prazo")
        void expiraNoPrazo() {
            var sessao = nova();

            assertThat(sessao.estaValida(AGORA.plus(Duration.ofDays(7)).minusSeconds(1))).isTrue();
            assertThat(sessao.estaValida(AGORA.plus(Duration.ofDays(7)).plusSeconds(1))).isFalse();
        }

        @Test
        @DisplayName("guarda o resumo, e nunca o token em si")
        void guardaSoOResumo() {
            var sessao = nova();

            assertThat(sessao.hashDoToken()).isEqualTo("hash-do-token");
        }
    }

    @Nested
    @DisplayName("Pedido de exclusão")
    class Exclusao {

        private PedidoDeExclusao novo() {
            return PedidoDeExclusao.novo(UUID.randomUUID(), UUID.randomUUID(), AGORA,
                    AGORA.plus(Duration.ofDays(15)));
        }

        @Test
        @DisplayName("nasce esperando os três serviços")
        void nasceEsperandoTodos() {
            var pedido = novo();

            assertThat(pedido.confirmados()).isEmpty();
            assertThat(pedido.faltando()).containsExactlyInAnyOrderElementsOf(Participante.todos());
            assertThat(pedido.estaCompleto()).isFalse();
        }

        @ParameterizedTest(name = "{0} confirma sozinho e não fecha a saga")
        @EnumSource(Participante.class)
        void umSoNaoFecha(Participante participante) {
            var pedido = novo();

            boolean fechou = pedido.confirmar(participante);

            assertThat(fechou).isFalse();
            assertThat(pedido.confirmados()).containsExactly(participante);
            assertThat(pedido.faltando()).hasSize(Participante.todos().size() - 1);
        }

        @Test
        @DisplayName("fecha quando o último confirma")
        void fechaComOUltimo() {
            var pedido = novo();
            var participantes = Participante.values();

            for (int posicao = 0; posicao < participantes.length - 1; posicao++) {
                assertThat(pedido.confirmar(participantes[posicao])).isFalse();
            }

            assertThat(pedido.confirmar(participantes[participantes.length - 1])).isTrue();
            assertThat(pedido.estaCompleto()).isTrue();
        }

        @Test
        @DisplayName("a mesma confirmação repetida não adianta nem atrasa")
        void confirmacaoRepetida() {
            var pedido = novo();

            pedido.confirmar(Participante.CATALOGO);
            pedido.confirmar(Participante.CATALOGO);
            pedido.confirmar(Participante.CATALOGO);

            assertThat(pedido.confirmados()).containsExactly(Participante.CATALOGO);
            assertThat(pedido.estaCompleto()).isFalse();
        }

        @Test
        @DisplayName("confirmação que chega depois de concluído não reabre nada")
        void confirmacaoDepoisDeConcluido() {
            var pedido = novo();
            Participante.todos().forEach(pedido::confirmar);
            pedido.concluir(AGORA.plusSeconds(5));

            boolean fechouDeNovo = pedido.confirmar(Participante.BUSCA);

            assertThat(fechouDeNovo).isFalse();
            assertThat(pedido.concluidoEm()).isEqualTo(AGORA.plusSeconds(5));
        }

        @Test
        @DisplayName("fica atrasado quando passa do prazo sem concluir")
        void ficaAtrasado() {
            var pedido = novo();

            assertThat(pedido.estaAtrasado(AGORA.plus(Duration.ofDays(14)))).isFalse();
            assertThat(pedido.estaAtrasado(AGORA.plus(Duration.ofDays(16)))).isTrue();
        }

        @Test
        @DisplayName("concluído no prazo nunca é atrasado")
        void concluidoNaoAtrasa() {
            var pedido = novo();
            pedido.concluir(AGORA.plusSeconds(30));

            assertThat(pedido.estaAtrasado(AGORA.plus(Duration.ofDays(90)))).isFalse();
        }

        @Test
        @DisplayName("guarda quando insistiu pela última vez")
        void guardaLembrete() {
            var pedido = novo();

            pedido.anotarLembrete(AGORA.plusSeconds(600));

            assertThat(pedido.ultimoLembreteEm()).isEqualTo(AGORA.plusSeconds(600));
        }

        @Test
        @DisplayName("a lista de participantes é a do contrato, e não uma cópia local")
        void participantesVemDoContrato() {
            assertThat(Participante.todos()).containsExactlyInAnyOrder(Participante.values());
        }
    }
}
