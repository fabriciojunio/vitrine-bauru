package br.com.vitrinebauru.cadastro.dominio;

import br.com.vitrinebauru.contratos.tipos.ApelidoNaUrl;
import br.com.vitrinebauru.contratos.tipos.Documento;
import br.com.vitrinebauru.contratos.tipos.Telefone;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Empreendedor")
class EmpreendedorTest {

    private static final Instant AGORA = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID MODERADOR = UUID.randomUUID();
    private static final String MOTIVO = "O documento informado não confere com o nome do negócio.";

    private Empreendedor novo() {
        return Empreendedor.novo(
                UUID.randomUUID(),
                "Doces da Lourdes",
                ApelidoNaUrl.deTexto("Doces da Lourdes"),
                "Bolo de pote e salgado de festa",
                "Alimentação",
                "Vila Cardia",
                "17011066",
                Telefone.de("14997123456"),
                Documento.de("52998224725"),
                AGORA);
    }

    private Empreendedor em(StatusDoCadastro status) {
        var empreendedor = novo();
        switch (status) {
            case PENDENTE -> {
            }
            case APROVADO -> empreendedor.aprovar(MODERADOR, AGORA);
            case REJEITADO -> empreendedor.rejeitar(MODERADOR, MOTIVO, AGORA);
            case SUSPENSO -> {
                empreendedor.aprovar(MODERADOR, AGORA);
                empreendedor.suspender(MODERADOR, MOTIVO, AGORA);
            }
            case EXCLUIDO -> empreendedor.marcarExcluido(AGORA);
        }
        return empreendedor;
    }

    @Nested
    @DisplayName("ao nascer")
    class AoNascer {

        @Test
        @DisplayName("entra na fila da SEDECON, e nao no ar")
        void nascePendente() {
            var empreendedor = novo();

            assertThat(empreendedor.status()).isEqualTo(StatusDoCadastro.PENDENTE);
            assertThat(empreendedor.status().apareceNaVitrine()).isFalse();
            assertThat(empreendedor.moderadoEm()).isNull();
            assertThat(empreendedor.moderadoPor()).isNull();
        }

        @Test
        @DisplayName("guarda telefone e documento so com digitos")
        void normalizaDados() {
            var empreendedor = novo();

            assertThat(empreendedor.telefoneWhatsapp()).isEqualTo("14997123456");
            assertThat(empreendedor.documento()).isEqualTo("52998224725");
            assertThat(empreendedor.documentoTipo()).isEqualTo(Documento.Tipo.CPF);
        }

        @Test
        @DisplayName("mostra o documento mascarado, mesmo para a moderacao")
        void mascaraDocumento() {
            assertThat(novo().documentoMascarado()).isEqualTo("***.982.247-**");
        }

        @Test
        @DisplayName("recusa bairro que nao e de Bauru")
        void recusaBairroDeFora() {
            assertThatThrownBy(() -> Empreendedor.novo(UUID.randomUUID(), "Loja",
                    ApelidoNaUrl.deTexto("Loja"), null, "Alimentação", "Copacabana", null,
                    Telefone.de("14997123456"), Documento.de("52998224725"), AGORA))
                    .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class)
                    .hasMessageContaining("Bairro não reconhecido");
        }

        @Test
        @DisplayName("recusa categoria inventada")
        void recusaCategoriaInventada() {
            assertThatThrownBy(() -> Empreendedor.novo(UUID.randomUUID(), "Loja",
                    ApelidoNaUrl.deTexto("Loja"), null, "Mineração", "Centro", null,
                    Telefone.de("14997123456"), Documento.de("52998224725"), AGORA))
                    .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class)
                    .hasMessageContaining("Categoria não reconhecida");
        }

        @ParameterizedTest(name = "aceita bairro escrito como \"{0}\"")
        @CsvSource({
                "'Vila Cardia', 'Vila Cardia'",
                "'vila cardia', 'Vila Cardia'",
                "'VILA CARDIA', 'Vila Cardia'",
                "'Tibirica', 'Tibiriçá'",
                "'nucleo habitacional mary dota', 'Núcleo Habitacional Mary Dota'",
                "'  Centro  ', 'Centro'"
        })
        @DisplayName("aceita o bairro sem exigir acento certo e guarda o nome oficial")
        void normalizaBairro(String digitado, String oficial) {
            var empreendedor = Empreendedor.novo(UUID.randomUUID(), "Loja",
                    ApelidoNaUrl.deTexto("Loja"), null, "Alimentação", digitado, null,
                    Telefone.de("14997123456"), Documento.de("52998224725"), AGORA);

            assertThat(empreendedor.bairro()).isEqualTo(oficial);
        }
    }

    @Nested
    @DisplayName("aprovacao")
    class Aprovacao {

        @Test
        @DisplayName("coloca a loja no ar e guarda quem aprovou")
        void aprova() {
            var empreendedor = novo();
            var quando = AGORA.plus(2, ChronoUnit.DAYS);

            empreendedor.aprovar(MODERADOR, quando);

            assertThat(empreendedor.status()).isEqualTo(StatusDoCadastro.APROVADO);
            assertThat(empreendedor.status().apareceNaVitrine()).isTrue();
            assertThat(empreendedor.moderadoPor()).isEqualTo(MODERADOR);
            assertThat(empreendedor.moderadoEm()).isEqualTo(quando);
        }

        @Test
        @DisplayName("nao deixa aprovar duas vezes")
        void naoAprovaDuasVezes() {
            var empreendedor = em(StatusDoCadastro.APROVADO);

            assertThatThrownBy(() -> empreendedor.aprovar(MODERADOR, AGORA))
                    .isInstanceOf(ErrosDeNegocio.Conflito.class)
                    .hasMessageContaining("aprovado");
        }

        @Test
        @DisplayName("limpa o motivo da recusa anterior")
        void limpaMotivoAnterior() {
            var empreendedor = em(StatusDoCadastro.REJEITADO);
            empreendedor.reenviarParaAnalise(AGORA);

            empreendedor.aprovar(MODERADOR, AGORA);

            assertThat(empreendedor.motivoDaModeracao()).isNull();
        }
    }

    @Nested
    @DisplayName("recusa e suspensao")
    class RecusaESuspensao {

        @Test
        @DisplayName("guarda o motivo escrito pelo analista")
        void guardaMotivo() {
            var empreendedor = novo();

            empreendedor.rejeitar(MODERADOR, MOTIVO, AGORA);

            assertThat(empreendedor.status()).isEqualTo(StatusDoCadastro.REJEITADO);
            assertThat(empreendedor.motivoDaModeracao()).isEqualTo(MOTIVO);
        }

        @ParameterizedTest(name = "recusa motivo \"{0}\"")
        @CsvSource({"''", "'   '", "'curto'", "'123456789'"})
        @DisplayName("exige motivo de verdade, porque ele vai no e-mail do empreendedor")
        void exigeMotivo(String motivo) {
            var empreendedor = novo();

            assertThatThrownBy(() -> empreendedor.rejeitar(MODERADOR, motivo, AGORA))
                    .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class)
                    .hasMessageContaining("motivo");
        }

        @Test
        @DisplayName("nao aceita motivo nulo")
        void recusaMotivoNulo() {
            var empreendedor = novo();

            assertThatThrownBy(() -> empreendedor.rejeitar(MODERADOR, null, AGORA))
                    .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class);
        }

        @Test
        @DisplayName("tira do ar quem estava no ar")
        void suspende() {
            var empreendedor = em(StatusDoCadastro.APROVADO);

            empreendedor.suspender(MODERADOR, "Denúncia de propaganda enganosa", AGORA);

            assertThat(empreendedor.status()).isEqualTo(StatusDoCadastro.SUSPENSO);
            assertThat(empreendedor.status().apareceNaVitrine()).isFalse();
        }

        @Test
        @DisplayName("nao suspende quem nunca foi aprovado")
        void naoSuspendePendente() {
            var empreendedor = novo();

            assertThatThrownBy(() -> empreendedor.suspender(MODERADOR, MOTIVO, AGORA))
                    .isInstanceOf(ErrosDeNegocio.Conflito.class);
        }

        @Test
        @DisplayName("devolve ao ar quem foi suspenso")
        void reativa() {
            var empreendedor = em(StatusDoCadastro.SUSPENSO);

            empreendedor.reativar(MODERADOR, AGORA);

            assertThat(empreendedor.status()).isEqualTo(StatusDoCadastro.APROVADO);
            assertThat(empreendedor.motivoDaModeracao()).isNull();
        }
    }

    @Nested
    @DisplayName("correcao depois da recusa")
    class CorrecaoDepoisDaRecusa {

        @Test
        @DisplayName("volta para a fila e zera a moderacao anterior")
        void voltaParaAFila() {
            var empreendedor = em(StatusDoCadastro.REJEITADO);

            empreendedor.reenviarParaAnalise(AGORA.plus(1, ChronoUnit.DAYS));

            assertThat(empreendedor.status()).isEqualTo(StatusDoCadastro.PENDENTE);
            assertThat(empreendedor.moderadoEm()).isNull();
            assertThat(empreendedor.moderadoPor()).isNull();
        }

        @Test
        @DisplayName("so vale para cadastro rejeitado")
        void soValeParaRejeitado() {
            var empreendedor = em(StatusDoCadastro.APROVADO);

            assertThatThrownBy(() -> empreendedor.reenviarParaAnalise(AGORA))
                    .isInstanceOf(ErrosDeNegocio.Conflito.class);
        }
    }

    @Nested
    @DisplayName("matriz de transicoes")
    class MatrizDeTransicoes {

        @ParameterizedTest(name = "aprovar em {0}")
        @EnumSource(StatusDoCadastro.class)
        void aprovarSoValeEmPendente(StatusDoCadastro origem) {
            var empreendedor = em(origem);

            if (origem == StatusDoCadastro.PENDENTE) {
                assertThatCode(() -> empreendedor.aprovar(MODERADOR, AGORA)).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> empreendedor.aprovar(MODERADOR, AGORA))
                        .isInstanceOf(ErrosDeNegocio.Conflito.class);
            }
        }

        @ParameterizedTest(name = "rejeitar em {0}")
        @EnumSource(StatusDoCadastro.class)
        void rejeitarSoValeEmPendente(StatusDoCadastro origem) {
            var empreendedor = em(origem);

            if (origem == StatusDoCadastro.PENDENTE) {
                assertThatCode(() -> empreendedor.rejeitar(MODERADOR, MOTIVO, AGORA))
                        .doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> empreendedor.rejeitar(MODERADOR, MOTIVO, AGORA))
                        .isInstanceOf(ErrosDeNegocio.Conflito.class);
            }
        }

        @ParameterizedTest(name = "suspender em {0}")
        @EnumSource(StatusDoCadastro.class)
        void suspenderSoValeEmAprovado(StatusDoCadastro origem) {
            var empreendedor = em(origem);

            if (origem == StatusDoCadastro.APROVADO) {
                assertThatCode(() -> empreendedor.suspender(MODERADOR, MOTIVO, AGORA))
                        .doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> empreendedor.suspender(MODERADOR, MOTIVO, AGORA))
                        .isInstanceOf(ErrosDeNegocio.Conflito.class);
            }
        }

        @ParameterizedTest(name = "reativar em {0}")
        @EnumSource(StatusDoCadastro.class)
        void reativarSoValeEmSuspenso(StatusDoCadastro origem) {
            var empreendedor = em(origem);

            if (origem == StatusDoCadastro.SUSPENSO) {
                assertThatCode(() -> empreendedor.reativar(MODERADOR, AGORA)).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> empreendedor.reativar(MODERADOR, AGORA))
                        .isInstanceOf(ErrosDeNegocio.Conflito.class);
            }
        }

        @ParameterizedTest(name = "excluir em {0}")
        @EnumSource(StatusDoCadastro.class)
        void excluirValeEmTudoMenosExcluido(StatusDoCadastro origem) {
            var empreendedor = em(origem);

            if (origem == StatusDoCadastro.EXCLUIDO) {
                assertThatThrownBy(() -> empreendedor.marcarExcluido(AGORA))
                        .isInstanceOf(ErrosDeNegocio.Conflito.class);
            } else {
                assertThatCode(() -> empreendedor.marcarExcluido(AGORA)).doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("edicao do perfil")
    class EdicaoDoPerfil {

        @Test
        @DisplayName("altera os dados sem devolver o cadastro para a fila")
        void naoVoltaParaFila() {
            var empreendedor = em(StatusDoCadastro.APROVADO);

            empreendedor.atualizarPerfil("Doces da Lourdes e Filhas", "Agora com salgado assado",
                    "Alimentação", "Vila Falcão", "17011066", Telefone.de("14997654321"),
                    AGORA.plus(5, ChronoUnit.DAYS));

            assertThat(empreendedor.status()).isEqualTo(StatusDoCadastro.APROVADO);
            assertThat(empreendedor.nomeDoNegocio()).isEqualTo("Doces da Lourdes e Filhas");
            assertThat(empreendedor.bairro()).isEqualTo("Vila Falcão");
        }

        @Test
        @DisplayName("nao muda o endereco publico da loja quando o nome muda")
        void mantemApelido() {
            var empreendedor = em(StatusDoCadastro.APROVADO);
            String antes = empreendedor.apelidoNaUrl();

            empreendedor.atualizarPerfil("Outro Nome Completamente Diferente", null,
                    "Artesanato", "Centro", null, Telefone.de("14997654321"), AGORA);

            assertThat(empreendedor.apelidoNaUrl()).isEqualTo(antes);
        }

        @Test
        @DisplayName("cadastro excluido nao aceita alteracao")
        void excluidoNaoEdita() {
            var empreendedor = em(StatusDoCadastro.EXCLUIDO);

            assertThatThrownBy(() -> empreendedor.atualizarPerfil("Nome", null, "Alimentação",
                    "Centro", null, Telefone.de("14997654321"), AGORA))
                    .isInstanceOf(ErrosDeNegocio.Proibido.class);
        }
    }

    @Nested
    @DisplayName("exclusao de dados")
    class ExclusaoDeDados {

        @Test
        @DisplayName("apaga o que identifica e mantem a linha para a auditoria")
        void anonimiza() {
            var empreendedor = em(StatusDoCadastro.APROVADO);
            UUID id = empreendedor.id();

            empreendedor.marcarExcluido(AGORA);
            empreendedor.anonimizar(AGORA);

            assertThat(empreendedor.id()).isEqualTo(id);
            assertThat(empreendedor.nomeDoNegocio()).isEqualTo("Cadastro removido");
            assertThat(empreendedor.descricao()).isNull();
            assertThat(empreendedor.documento()).doesNotContain("52998224725");
            assertThat(empreendedor.telefoneWhatsapp()).isEqualTo("00000000000");
            assertThat(empreendedor.status()).isEqualTo(StatusDoCadastro.EXCLUIDO);
        }

        @Test
        @DisplayName("o apelido anonimizado nao colide com loja nenhuma")
        void apelidoAnonimizadoNaoColide() {
            var empreendedor = novo();

            empreendedor.marcarExcluido(AGORA);
            empreendedor.anonimizar(AGORA);

            assertThat(empreendedor.apelidoNaUrl()).startsWith("removido-");
            assertThat(empreendedor.apelidoNaUrl()).contains(empreendedor.id().toString());
        }
    }

    @Nested
    @DisplayName("conferencia do documento")
    class ConferenciaDoDocumento {

        @Test
        @DisplayName("guarda o resultado da consulta sem mudar a situacao do cadastro")
        void anotaSemDecidir() {
            var empreendedor = novo();

            empreendedor.anotarConferenciaDoDocumento("ATIVA (DOCES DA LOURDES LTDA)", AGORA);

            assertThat(empreendedor.situacaoDoDocumento()).contains("ATIVA");
            assertThat(empreendedor.documentoConferidoEm()).isEqualTo(AGORA);
            assertThat(empreendedor.status())
                    .as("consulta a Receita informa, nao decide")
                    .isEqualTo(StatusDoCadastro.PENDENTE);
        }
    }

    @Nested
    @DisplayName("regras do enum de situacao")
    class RegrasDoEnum {

        @ParameterizedTest(name = "{0}")
        @EnumSource(StatusDoCadastro.class)
        @DisplayName("so aprovado aparece na vitrine")
        void soAprovadoApareceNaVitrine(StatusDoCadastro status) {
            assertThat(status.apareceNaVitrine()).isEqualTo(status == StatusDoCadastro.APROVADO);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(StatusDoCadastro.class)
        @DisplayName("aprovado e pendente podem mexer no catalogo")
        void quemPodeMexerNoCatalogo(StatusDoCadastro status) {
            boolean esperado = status == StatusDoCadastro.APROVADO || status == StatusDoCadastro.PENDENTE;

            assertThat(status.permiteEditarCatalogo()).isEqualTo(esperado);
        }
    }
}
