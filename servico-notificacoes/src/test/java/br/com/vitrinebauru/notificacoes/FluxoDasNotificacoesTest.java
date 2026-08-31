package br.com.vitrinebauru.notificacoes;

import br.com.vitrinebauru.contratos.CadastroAprovado;
import br.com.vitrinebauru.contratos.CadastroRejeitado;
import br.com.vitrinebauru.contratos.EmpreendedorCadastrado;
import br.com.vitrinebauru.contratos.EmpreendedorReativado;
import br.com.vitrinebauru.contratos.EmpreendedorSuspenso;
import br.com.vitrinebauru.contratos.ExclusaoSolicitada;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.notificacoes.dominio.Notificacao;
import br.com.vitrinebauru.notificacoes.dominio.TipoDeNotificacao;
import br.com.vitrinebauru.notificacoes.infraestrutura.envio.EnviadorDeEmail;
import br.com.vitrinebauru.notificacoes.infraestrutura.persistencia.NotificacaoRepository;
import br.com.vitrinebauru.plataforma.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Evento entra, e-mail sai.
 *
 * <p>O envio de verdade é trocado por um enviador controlado pelo teste, que
 * permite simular o provedor fora do ar. Sem isso, o teste da nova tentativa
 * dependeria de derrubar a internet.
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {Topicos.EMPREENDEDORES, Topicos.CATALOGO, Topicos.CONTATOS, Topicos.PRIVACIDADE,
                Topicos.EMPREENDEDORES + ".dlq", Topicos.CATALOGO + ".dlq",
                Topicos.CONTATOS + ".dlq", Topicos.PRIVACIDADE + ".dlq"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
@DisplayName("Fluxo das notificações")
class FluxoDasNotificacoesTest {

    private static final Duration ESPERA = Duration.ofSeconds(20);
    private static EmbeddedPostgres postgres;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry propriedades) throws Exception {
        postgres = EmbeddedPostgres.builder().start();
        propriedades.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        propriedades.add("spring.datasource.username", () -> "postgres");
        propriedades.add("spring.datasource.password", () -> "");
        propriedades.add("spring.kafka.bootstrap-servers", () -> "${spring.embedded.kafka.brokers}");
        propriedades.add("vitrine.outbox.intervalo-ms", () -> "100");
        propriedades.add("vitrine.email.intervalo-ms", () -> "200");
    }

    /**
     * Enviador de mentira: guarda o que foi mandado e sabe fingir queda do
     * provedor.
     */
    static class EnviadorDeTeste implements EnviadorDeEmail {

        final List<Notificacao> enviados = new ArrayList<>();
        final AtomicBoolean quebrado = new AtomicBoolean(false);

        @Override
        public void enviar(Notificacao notificacao) throws Exception {
            if (quebrado.get()) {
                throw new IllegalStateException("provedor de e-mail fora do ar");
            }
            enviados.add(notificacao);
        }

        @Override
        public String descricao() {
            return "teste";
        }
    }

    @TestConfiguration
    static class Configuracao {

        @Bean
        @Primary
        EnviadorDeTeste enviadorDeTeste() {
            return new EnviadorDeTeste();
        }
    }

    @Autowired
    private ObjectMapper json;
    @Autowired
    private KafkaTemplate<String, String> kafka;
    @Autowired
    private NotificacaoRepository notificacoes;
    @Autowired
    private OutboxRepository outbox;
    @Autowired
    private EnviadorDeTeste enviador;

    private UUID empreendedorId;

    @BeforeEach
    void preparar() {
        notificacoes.deleteAll();
        outbox.deleteAll();
        enviador.enviados.clear();
        enviador.quebrado.set(false);
        empreendedorId = UUID.randomUUID();
    }

    private void publicar(Object evento) throws Exception {
        kafka.send(Topicos.EMPREENDEDORES, empreendedorId.toString(), json.writeValueAsString(evento));
    }

    private EmpreendedorCadastrado cadastro() {
        return new EmpreendedorCadastrado(UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                empreendedorId, UUID.randomUUID(), "Doces da Lourdes", "doces-da-lourdes",
                "Bolo de pote", "Alimentação", "Vila Cardia", "14997010101",
                "52998224725", "lourdes@exemplo.com", "Maria de Lourdes");
    }

    private CadastroAprovado aprovacao() {
        return new CadastroAprovado(UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                empreendedorId, UUID.randomUUID(), "Doces da Lourdes",
                "lourdes@exemplo.com", "Maria de Lourdes");
    }

    @Nested
    @DisplayName("evento vira e-mail")
    class EventoViraEmail {

        @Test
        @DisplayName("cadastro gera boas-vindas explicando a fila da SEDECON")
        void cadastroGeraBoasVindas() throws Exception {
            publicar(cadastro());

            await().atMost(ESPERA).untilAsserted(() -> {
                assertThat(enviador.enviados).hasSize(1);
                assertThat(enviador.enviados.getFirst().tipo())
                        .isEqualTo(TipoDeNotificacao.BOAS_VINDAS);
                assertThat(enviador.enviados.getFirst().destinatario())
                        .isEqualTo("lourdes@exemplo.com");
                assertThat(enviador.enviados.getFirst().corpo()).contains("análise");
            });
        }

        @Test
        @DisplayName("aprovação gera o e-mail que avisa que a loja está no ar")
        void aprovacaoGeraEmail() throws Exception {
            publicar(aprovacao());

            await().atMost(ESPERA).untilAsserted(() -> {
                assertThat(enviador.enviados).hasSize(1);
                assertThat(enviador.enviados.getFirst().assunto()).contains("no ar");
            });
        }

        @Test
        @DisplayName("recusa leva o motivo escrito pela análise")
        void recusaLevaOMotivo() throws Exception {
            String motivo = "O documento informado não confere com o nome do negócio.";

            publicar(new CadastroRejeitado(UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                    empreendedorId, UUID.randomUUID(), motivo, "Doces da Lourdes",
                    "lourdes@exemplo.com", "Maria de Lourdes"));

            await().atMost(ESPERA).untilAsserted(() -> {
                assertThat(enviador.enviados).hasSize(1);
                assertThat(enviador.enviados.getFirst().corpo()).contains(motivo);
            });
        }

        @Test
        @DisplayName("suspensão avisa e diz que os produtos continuam salvos")
        void suspensaoAvisa() throws Exception {
            publicar(new EmpreendedorSuspenso(UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                    empreendedorId, UUID.randomUUID(), "Denúncia em análise", "Doces da Lourdes",
                    "lourdes@exemplo.com", "Maria de Lourdes"));

            await().atMost(ESPERA).untilAsserted(() -> {
                assertThat(enviador.enviados).hasSize(1);
                assertThat(enviador.enviados.getFirst().corpo()).contains("continuam salvos");
            });
        }

        @Test
        @DisplayName("reativação não gera e-mail, de propósito")
        void reativacaoNaoGeraEmail() throws Exception {
            publicar(new EmpreendedorReativado(UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                    empreendedorId, UUID.randomUUID(), "Doces da Lourdes",
                    "lourdes@exemplo.com", "Maria de Lourdes"));

            publicar(aprovacao());

            await().atMost(ESPERA).untilAsserted(() -> assertThat(enviador.enviados).hasSize(1));
            assertThat(enviador.enviados.getFirst().tipo())
                    .isEqualTo(TipoDeNotificacao.CADASTRO_APROVADO);
        }

        @Test
        @DisplayName("o mesmo evento reentregue não manda dois e-mails")
        void eventoRepetidoNaoDuplica() throws Exception {
            var evento = aprovacao();
            String carga = json.writeValueAsString(evento);

            kafka.send(Topicos.EMPREENDEDORES, empreendedorId.toString(), carga);
            kafka.send(Topicos.EMPREENDEDORES, empreendedorId.toString(), carga);
            kafka.send(Topicos.EMPREENDEDORES, empreendedorId.toString(), carga);

            await().atMost(ESPERA).untilAsserted(() -> assertThat(enviador.enviados).hasSize(1));
            assertThat(notificacoes.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("quando o provedor cai")
    class QuandoOProvedorCai {

        @Test
        @DisplayName("a mensagem fica na fila com a falha registrada, e não se perde")
        void ficaNaFila() throws Exception {
            enviador.quebrado.set(true);

            publicar(aprovacao());

            await().atMost(ESPERA).untilAsserted(() -> {
                var pendentes = notificacoes.findAll();
                assertThat(pendentes).hasSize(1);
                assertThat(pendentes.getFirst().foiEnviada()).isFalse();
                assertThat(pendentes.getFirst().tentativas()).isPositive();
                assertThat(pendentes.getFirst().ultimoErro()).contains("fora do ar");
            });

            assertThat(enviador.enviados).isEmpty();
        }

        @Test
        @DisplayName("a fila pendente vira métrica, para o problema aparecer antes da reclamação")
        void filaViraMetrica() throws Exception {
            enviador.quebrado.set(true);
            publicar(aprovacao());

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(notificacoes.countByEnviadaEmIsNull()).isEqualTo(1));
        }
    }

    @Nested
    @DisplayName("exclusão de dados (LGPD)")
    class Exclusao {

        @Test
        @DisplayName("apaga o histórico de e-mail e confirma o expurgo")
        void apagaEConfirma() throws Exception {
            publicar(cadastro());
            publicar(aprovacao());

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(notificacoes.count()).isEqualTo(2));

            kafka.send(Topicos.PRIVACIDADE, empreendedorId.toString(),
                    json.writeValueAsString(new ExclusaoSolicitada(
                            UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                            empreendedorId, UUID.randomUUID(),
                            Instant.now().plusSeconds(1_296_000))));

            await().atMost(ESPERA).untilAsserted(() -> {
                assertThat(notificacoes.findByEmpreendedorIdOrderByCriadaEmDesc(empreendedorId))
                        .isEmpty();
                assertThat(outbox.findAll())
                        .anyMatch(mensagem -> mensagem.tipo().equals("ExpurgoConcluido"));
            });
        }

        @Test
        @DisplayName("expurgo sem nada para apagar confirma do mesmo jeito")
        void expurgoVazioConfirma() throws Exception {
            kafka.send(Topicos.PRIVACIDADE, empreendedorId.toString(),
                    json.writeValueAsString(new ExclusaoSolicitada(
                            UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                            empreendedorId, UUID.randomUUID(),
                            Instant.now().plusSeconds(1_296_000))));

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(outbox.findAll())
                            .anyMatch(mensagem -> mensagem.tipo().equals("ExpurgoConcluido")));
        }
    }
}
