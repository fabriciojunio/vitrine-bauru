package br.com.vitrinebauru.cadastro;

import br.com.vitrinebauru.cadastro.dominio.StatusDoCadastro;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.AuditoriaRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.PedidoDeExclusaoRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.SessaoDeRenovacaoRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository;
import br.com.vitrinebauru.contratos.ExpurgoConcluido;
import br.com.vitrinebauru.contratos.Participante;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.inbox.EventoProcessadoRepository;
import br.com.vitrinebauru.plataforma.outbox.OutboxRepository;
import br.com.vitrinebauru.plataforma.seguranca.Papel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe o servico inteiro, com PostgreSQL e Kafka de verdade, e percorre o
 * caminho que um empreendedor de Bauru percorreria.
 *
 * <p>Nenhum dos dois precisa de Docker: o Postgres e um processo iniciado pelo
 * proprio teste e o Kafka e o broker embutido do spring-kafka-test. Isso
 * importa porque o teste que prova o outbox de ponta a ponta so vale se rodar
 * a cada build, e nao quando alguem lembra de subir a infraestrutura.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EmbeddedKafka(
        partitions = 1,
        topics = {Topicos.EMPREENDEDORES, Topicos.CATALOGO, Topicos.CONTATOS, Topicos.PRIVACIDADE,
                Topicos.EMPREENDEDORES + ".dlq", Topicos.CATALOGO + ".dlq",
                Topicos.CONTATOS + ".dlq", Topicos.PRIVACIDADE + ".dlq"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
@DisplayName("Fluxo do cadastro, de ponta a ponta")
class FluxoDoCadastroTest {

    private static final Duration ESPERA = Duration.ofSeconds(20);
    private static EmbeddedPostgres postgres;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry propriedades) throws Exception {
        postgres = EmbeddedPostgres.builder().start();
        propriedades.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        propriedades.add("spring.datasource.username", () -> "postgres");
        propriedades.add("spring.datasource.password", () -> "");
        propriedades.add("spring.kafka.bootstrap-servers", () -> "${spring.embedded.kafka.brokers}");

        // Publica rapido para o teste nao esperar meio segundo por mensagem.
        propriedades.add("vitrine.outbox.intervalo-ms", () -> "100");
        // A consulta a Receita nao entra em teste: depende de rede e de um
        // servico de terceiro, e o que se quer verificar aqui e o fluxo.
        propriedades.add("vitrine.brasilapi.ativa", () -> "false");
        propriedades.add("vitrine.conferencia.intervalo-ms", () -> "3600000");
        propriedades.add("vitrine.limite.ativo", () -> "false");
    }

    // O Postgres embutido nao e fechado aqui de proposito. Fechar no @AfterAll
    // derruba o banco antes de o contexto do Spring parar, e as tarefas
    // agendadas que ainda estao rodando enchem a saida de erro de conexao. O
    // proprio processo do banco morre junto com a maquina virtual do teste.

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private EmpreendedorRepository empreendedores;
    @Autowired
    private SessaoDeRenovacaoRepository sessoes;
    @Autowired
    private AuditoriaRepository auditoria;
    @Autowired
    private PedidoDeExclusaoRepository pedidosDeExclusao;
    @Autowired
    private OutboxRepository outbox;
    @Autowired
    private EventoProcessadoRepository inbox;
    @Autowired
    private KafkaTemplate<String, String> kafka;
    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder codificador;

    @BeforeEach
    void limpar() {
        pedidosDeExclusao.deleteAll();
        sessoes.deleteAll();
        auditoria.deleteAll();
        empreendedores.deleteAll();
        usuarios.deleteAll();
        outbox.deleteAll();
        inbox.deleteAll();
    }

    // ---------------------------------------------------------------- ajudas

    private String corpoDeCadastro(String email, String documento, String nomeDoNegocio) {
        return """
                {
                  "nome": "Maria de Lourdes Prado",
                  "email": "%s",
                  "senha": "bolodefuba2026",
                  "nomeDoNegocio": "%s",
                  "descricao": "Bolo de pote e salgado de festa por encomenda",
                  "categoriaPrincipal": "Alimentação",
                  "bairro": "Vila Cardia",
                  "cep": "17011-066",
                  "telefoneWhatsapp": "(14) 99712-3456",
                  "documento": "%s"
                }
                """.formatted(email, nomeDoNegocio, documento);
    }

    private UUID cadastrar(String email, String documento, String nomeDoNegocio) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/cadastro/empreendedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCadastro(email, documento, nomeDoNegocio)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(ler(resultado).get("empreendedorId").asText());
    }

    private JsonNode ler(MvcResult resultado) throws Exception {
        return json.readTree(resultado.getResponse().getContentAsString());
    }

    private String entrar(String email, String senha) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/cadastro/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn();

        return ler(resultado).get("tokenDeAcesso").asText();
    }

    private String criarAdministrador() throws Exception {
        var admin = usuarios.save(br.com.vitrinebauru.cadastro.dominio.Usuario.novo(
                "Analista SEDECON", "analista@bauru.sp.gov.br",
                codificador.encode("moderacao2026"), Papel.ADMIN_SEDECON, Instant.now()));
        return entrar(admin.email(), "moderacao2026");
    }

    // ------------------------------------------------------------- cadastro

    @Nested
    @DisplayName("cadastro do empreendedor")
    class CadastroDoEmpreendedor {

        @Test
        @DisplayName("cria conta, loja pendente e evento no outbox, tudo na mesma transação")
        void cadastroCompleto() throws Exception {
            UUID empreendedorId = cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");

            var empreendedor = empreendedores.findById(empreendedorId).orElseThrow();
            assertThat(empreendedor.status()).isEqualTo(StatusDoCadastro.PENDENTE);
            assertThat(empreendedor.apelidoNaUrl()).isEqualTo("doces-da-lourdes");
            assertThat(empreendedor.bairro()).isEqualTo("Vila Cardia");
            assertThat(usuarios.findByEmail("lourdes@exemplo.com")).isPresent();
            assertThat(auditoria.findByEntidadeIdOrderByOcorridoEmDesc(empreendedorId)).isNotEmpty();
        }

        @Test
        @DisplayName("guarda a senha com bcrypt, nunca em texto puro")
        void senhaComBcrypt() throws Exception {
            cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");

            var usuario = usuarios.findByEmail("lourdes@exemplo.com").orElseThrow();

            assertThat(usuario.senhaHash()).startsWith("$2");
            assertThat(usuario.senhaHash()).doesNotContain("bolodefuba2026");
            assertThat(codificador.matches("bolodefuba2026", usuario.senhaHash())).isTrue();
        }

        @Test
        @DisplayName("o evento sai do outbox e chega ao broker")
        void eventoChegaAoBroker() throws Exception {
            cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");

            await().atMost(ESPERA).untilAsserted(() -> {
                var mensagens = outbox.findAll();
                assertThat(mensagens).hasSize(1);
                assertThat(mensagens.getFirst().tipo()).isEqualTo("EmpreendedorCadastrado");
                assertThat(mensagens.getFirst().foiPublicada())
                        .as("o publicador precisa marcar como publicada depois do envio")
                        .isTrue();
            });
        }

        @Test
        @DisplayName("recusa e-mail já cadastrado")
        void recusaEmailRepetido() throws Exception {
            cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");

            mockMvc.perform(post("/api/cadastro/empreendedores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoDeCadastro("lourdes@exemplo.com", "11222333000181", "Outra Loja")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("e-mail")));
        }

        @Test
        @DisplayName("recusa o mesmo CPF em duas lojas")
        void recusaDocumentoRepetido() throws Exception {
            cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");

            mockMvc.perform(post("/api/cadastro/empreendedores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoDeCadastro("outro@exemplo.com", "52998224725", "Outra Loja")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail")
                            .value(org.hamcrest.Matchers.containsString("já está cadastrado")));
        }

        @Test
        @DisplayName("duas lojas com o mesmo nome ganham endereços diferentes")
        void apelidoGanhaSufixo() throws Exception {
            cadastrar("um@exemplo.com", "52998224725", "Casa do Pastel");
            UUID segunda = cadastrar("dois@exemplo.com", "11222333000181", "Casa do Pastel");

            assertThat(empreendedores.findById(segunda).orElseThrow().apelidoNaUrl())
                    .isEqualTo("casa-do-pastel-2");
        }

        @Test
        @DisplayName("recusa senha fraca com mensagem que explica o que fazer")
        void recusaSenhaFraca() throws Exception {
            String corpo = corpoDeCadastro("lourdes@exemplo.com", "52998224725", "Doces")
                    .replace("bolodefuba2026", "12345678");

            mockMvc.perform(post("/api/cadastro/empreendedores")
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail")
                            .value(org.hamcrest.Matchers.containsString("fácil de adivinhar")));
        }

        @Test
        @DisplayName("recusa bairro que não é de Bauru")
        void recusaBairroDeFora() throws Exception {
            String corpo = corpoDeCadastro("lourdes@exemplo.com", "52998224725", "Doces")
                    .replace("Vila Cardia", "Copacabana");

            mockMvc.perform(post("/api/cadastro/empreendedores")
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("recusa documento com dígito verificador errado")
        void recusaDocumentoInvalido() throws Exception {
            mockMvc.perform(post("/api/cadastro/empreendedores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoDeCadastro("lourdes@exemplo.com", "52998224726", "Doces")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("recusa telefone fixo, porque o contato é pelo WhatsApp")
        void recusaTelefoneFixo() throws Exception {
            String corpo = corpoDeCadastro("lourdes@exemplo.com", "52998224725", "Doces")
                    .replace("(14) 99712-3456", "(14) 3227-7819");

            mockMvc.perform(post("/api/cadastro/empreendedores")
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail")
                            .value(org.hamcrest.Matchers.containsString("WhatsApp")));
        }

        @Test
        @DisplayName("aponta o campo que faltou, em português")
        void apontaCampoQueFaltou() throws Exception {
            mockMvc.perform(post("/api/cadastro/empreendedores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"sem-nome@exemplo.com\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Dados inválidos"))
                    .andExpect(jsonPath("$.campos.nome").exists())
                    .andExpect(jsonPath("$.campos.senha").exists());
        }

        @Test
        @DisplayName("limpa marcação HTML da descrição antes de guardar")
        void limpaHtmlDaDescricao() throws Exception {
            String corpo = corpoDeCadastro("lourdes@exemplo.com", "52998224725", "Doces da Lourdes")
                    .replace("Bolo de pote e salgado de festa por encomenda",
                            "Bolo <script>alert(1)</script> caseiro");

            MvcResult resultado = mockMvc.perform(post("/api/cadastro/empreendedores")
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isCreated())
                    .andReturn();

            var empreendedor = empreendedores
                    .findById(UUID.fromString(ler(resultado).get("empreendedorId").asText()))
                    .orElseThrow();

            assertThat(empreendedor.descricao()).doesNotContain("<script>");
            assertThat(empreendedor.descricao()).contains("Bolo");
        }

        @Test
        @DisplayName("a lista de bairros e a de categorias são públicas")
        void listasPublicas() throws Exception {
            mockMvc.perform(get("/api/cadastro/bairros"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]").value("Centro"));

            mockMvc.perform(get("/api/cadastro/categorias"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]").value("Alimentação"));
        }
    }

    // ---------------------------------------------------------------- login

    @Nested
    @DisplayName("entrada e sessão")
    class EntradaESessao {

        @Test
        @DisplayName("entra com a senha certa e recebe o par de tokens")
        void entraComSenhaCerta() throws Exception {
            cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");

            mockMvc.perform(post("/api/cadastro/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"lourdes@exemplo.com\",\"senha\":\"bolodefuba2026\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tokenDeAcesso").isNotEmpty())
                    .andExpect(jsonPath("$.tokenDeRenovacao").isNotEmpty())
                    .andExpect(jsonPath("$.usuario.papel").value("EMPREENDEDOR"))
                    .andExpect(jsonPath("$.usuario.empreendedorId").isNotEmpty());
        }

        @Test
        @DisplayName("aceita o e-mail digitado com maiúsculas")
        void aceitaEmailComMaiuscula() throws Exception {
            cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");

            mockMvc.perform(post("/api/cadastro/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"Lourdes@Exemplo.COM\",\"senha\":\"bolodefuba2026\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("dá a mesma resposta para senha errada e para e-mail inexistente")
        void mesmaRespostaParaOsDoisErros() throws Exception {
            cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");

            MvcResult senhaErrada = mockMvc.perform(post("/api/cadastro/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"lourdes@exemplo.com\",\"senha\":\"errada12345\"}"))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            MvcResult naoExiste = mockMvc.perform(post("/api/cadastro/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"ninguem@exemplo.com\",\"senha\":\"errada12345\"}"))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            assertThat(ler(senhaErrada).get("detail").asText())
                    .isEqualTo(ler(naoExiste).get("detail").asText());
        }

        @Test
        @DisplayName("bloqueia a conta depois de cinco senhas erradas")
        void bloqueiaDepoisDeCinco() throws Exception {
            cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");

            for (int tentativa = 0; tentativa < 5; tentativa++) {
                mockMvc.perform(post("/api/cadastro/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"lourdes@exemplo.com\",\"senha\":\"errada12345\"}"))
                        .andExpect(status().isUnauthorized());
            }

            mockMvc.perform(post("/api/cadastro/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"lourdes@exemplo.com\",\"senha\":\"bolodefuba2026\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail")
                            .value(org.hamcrest.Matchers.containsString("bloqueada")));
        }

        @Test
        @DisplayName("renova a sessão e queima o token antigo")
        void renovaEQueimaOAntigo() throws Exception {
            cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");

            MvcResult login = mockMvc.perform(post("/api/cadastro/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"lourdes@exemplo.com\",\"senha\":\"bolodefuba2026\"}"))
                    .andReturn();
            String renovacao = ler(login).get("tokenDeRenovacao").asText();

            mockMvc.perform(post("/api/cadastro/auth/renovar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tokenDeRenovacao\":\"%s\"}".formatted(renovacao)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tokenDeAcesso").isNotEmpty());

            mockMvc.perform(post("/api/cadastro/auth/renovar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tokenDeRenovacao\":\"%s\"}".formatted(renovacao)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail")
                            .value(org.hamcrest.Matchers.containsString("uso indevido")));
        }

        @Test
        @DisplayName("reuso de token derruba todas as sessões daquela conta")
        void reusoDerrubaTudo() throws Exception {
            cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");

            MvcResult login = mockMvc.perform(post("/api/cadastro/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"lourdes@exemplo.com\",\"senha\":\"bolodefuba2026\"}"))
                    .andReturn();
            String primeiro = ler(login).get("tokenDeRenovacao").asText();

            MvcResult renovado = mockMvc.perform(post("/api/cadastro/auth/renovar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tokenDeRenovacao\":\"%s\"}".formatted(primeiro)))
                    .andReturn();
            String segundo = ler(renovado).get("tokenDeRenovacao").asText();

            mockMvc.perform(post("/api/cadastro/auth/renovar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tokenDeRenovacao\":\"%s\"}".formatted(primeiro)))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/api/cadastro/auth/renovar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tokenDeRenovacao\":\"%s\"}".formatted(segundo)))
                    .andExpect(status().isUnauthorized());

            assertThat(auditoria.findAll())
                    .anyMatch(registro -> registro.acao().equals("reuso_de_token_detectado"));
        }

        @Test
        @DisplayName("sair revoga a renovação")
        void sairRevoga() throws Exception {
            cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            MvcResult login = mockMvc.perform(post("/api/cadastro/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"lourdes@exemplo.com\",\"senha\":\"bolodefuba2026\"}"))
                    .andReturn();
            String renovacao = ler(login).get("tokenDeRenovacao").asText();

            mockMvc.perform(post("/api/cadastro/auth/sair")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tokenDeRenovacao\":\"%s\"}".formatted(renovacao)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/cadastro/auth/renovar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tokenDeRenovacao\":\"%s\"}".formatted(renovacao)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("sair sem sessão nenhuma não dá erro")
        void sairSemSessao() throws Exception {
            mockMvc.perform(post("/api/cadastro/auth/sair")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tokenDeRenovacao\":\"inexistente\"}"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("quem está logado consegue ver os próprios dados")
        void euComToken() throws Exception {
            cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            String token = entrar("lourdes@exemplo.com", "bolodefuba2026");

            mockMvc.perform(get("/api/cadastro/auth/eu").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("lourdes@exemplo.com"))
                    .andExpect(jsonPath("$.nome").value("Maria de Lourdes Prado"));
        }

        @Test
        @DisplayName("token inválido não passa")
        void tokenInvalido() throws Exception {
            mockMvc.perform(get("/api/cadastro/minha-loja")
                            .header("Authorization", "Bearer token.que.nao.vale"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("sem token, a área do empreendedor responde 401 em português")
        void semToken() throws Exception {
            mockMvc.perform(get("/api/cadastro/minha-loja"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Faça login para continuar."));
        }
    }

    // ------------------------------------------------------------ moderacao

    @Nested
    @DisplayName("moderação da SEDECON")
    class Moderacao {

        @Test
        @DisplayName("a fila mostra quem está esperando, do mais antigo para o mais novo")
        void filaOrdenada() throws Exception {
            cadastrar("um@exemplo.com", "52998224725", "Primeira Loja");
            cadastrar("dois@exemplo.com", "11222333000181", "Segunda Loja");
            String admin = criarAdministrador();

            mockMvc.perform(get("/api/cadastro/moderacao/fila").header("Authorization", "Bearer " + admin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(2))
                    .andExpect(jsonPath("$.conteudo[0].nomeDoNegocio").value("Primeira Loja"))
                    .andExpect(jsonPath("$.conteudo[0].documento")
                            .value(org.hamcrest.Matchers.startsWith("***")));
        }

        @Test
        @DisplayName("aprovar coloca a loja no ar e gera evento")
        void aprovar() throws Exception {
            UUID empreendedorId = cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            String admin = criarAdministrador();

            mockMvc.perform(post("/api/cadastro/moderacao/{id}/aprovar", empreendedorId)
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(status().isNoContent());

            assertThat(empreendedores.findById(empreendedorId).orElseThrow().status())
                    .isEqualTo(StatusDoCadastro.APROVADO);

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(outbox.findAll())
                            .anyMatch(mensagem -> mensagem.tipo().equals("CadastroAprovado")
                                    && mensagem.foiPublicada()));
        }

        @Test
        @DisplayName("rejeitar exige motivo escrito")
        void rejeitarExigeMotivo() throws Exception {
            UUID empreendedorId = cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            String admin = criarAdministrador();

            mockMvc.perform(post("/api/cadastro/moderacao/{id}/rejeitar", empreendedorId)
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"motivo\":\"curto\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("rejeitar guarda o motivo e ele aparece para o empreendedor")
        void rejeitarGuardaMotivo() throws Exception {
            UUID empreendedorId = cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            String admin = criarAdministrador();
            String motivo = "O documento informado não confere com o nome do negócio.";

            mockMvc.perform(post("/api/cadastro/moderacao/{id}/rejeitar", empreendedorId)
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"motivo\":\"%s\"}".formatted(motivo)))
                    .andExpect(status().isNoContent());

            String tokenDoDono = entrar("lourdes@exemplo.com", "bolodefuba2026");

            mockMvc.perform(get("/api/cadastro/minha-loja")
                            .header("Authorization", "Bearer " + tokenDoDono))
                    .andExpect(jsonPath("$.situacao").value("REJEITADO"))
                    .andExpect(jsonPath("$.motivoDaModeracao").value(motivo))
                    .andExpect(jsonPath("$.apareceNaVitrine").value(false));
        }

        @Test
        @DisplayName("aprovar duas vezes é conflito, e não erro genérico")
        void aprovarDuasVezes() throws Exception {
            UUID empreendedorId = cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            String admin = criarAdministrador();

            mockMvc.perform(post("/api/cadastro/moderacao/{id}/aprovar", empreendedorId)
                    .header("Authorization", "Bearer " + admin));

            mockMvc.perform(post("/api/cadastro/moderacao/{id}/aprovar", empreendedorId)
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("suspender e reativar percorrem o caminho todo")
        void suspenderEReativar() throws Exception {
            UUID empreendedorId = cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            String admin = criarAdministrador();

            mockMvc.perform(post("/api/cadastro/moderacao/{id}/aprovar", empreendedorId)
                    .header("Authorization", "Bearer " + admin));

            mockMvc.perform(post("/api/cadastro/moderacao/{id}/suspender", empreendedorId)
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"motivo\":\"Denúncia de propaganda enganosa em análise\"}"))
                    .andExpect(status().isNoContent());

            assertThat(empreendedores.findById(empreendedorId).orElseThrow().status())
                    .isEqualTo(StatusDoCadastro.SUSPENSO);

            mockMvc.perform(post("/api/cadastro/moderacao/{id}/reativar", empreendedorId)
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(status().isNoContent());

            assertThat(empreendedores.findById(empreendedorId).orElseThrow().status())
                    .isEqualTo(StatusDoCadastro.APROVADO);
        }

        @Test
        @DisplayName("empreendedor não entra na área da SEDECON")
        void empreendedorNaoModera() throws Exception {
            UUID empreendedorId = cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            String token = entrar("lourdes@exemplo.com", "bolodefuba2026");

            mockMvc.perform(get("/api/cadastro/moderacao/fila").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/cadastro/moderacao/{id}/aprovar", empreendedorId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("sem login, a moderação nem existe")
        void semLoginNaoModera() throws Exception {
            mockMvc.perform(get("/api/cadastro/moderacao/fila"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("cadastro rejeitado volta para a fila depois de corrigido")
        void corrigirEReenviar() throws Exception {
            UUID empreendedorId = cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            String admin = criarAdministrador();

            mockMvc.perform(post("/api/cadastro/moderacao/{id}/rejeitar", empreendedorId)
                    .header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\":\"A descrição não explica o que você vende.\"}"));

            String dono = entrar("lourdes@exemplo.com", "bolodefuba2026");

            mockMvc.perform(put("/api/cadastro/minha-loja")
                            .header("Authorization", "Bearer " + dono)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nomeDoNegocio": "Doces da Lourdes",
                                      "descricao": "Bolo de pote, brigadeiro e torta salgada por encomenda",
                                      "categoriaPrincipal": "Alimentação",
                                      "bairro": "Vila Cardia",
                                      "cep": "17011-066",
                                      "telefoneWhatsapp": "(14) 99712-3456"
                                    }
                                    """))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/cadastro/minha-loja/reenviar")
                            .header("Authorization", "Bearer " + dono))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("PENDENTE"));
        }

        @Test
        @DisplayName("o painel conta o que a SEDECON precisa saber")
        void painel() throws Exception {
            UUID primeira = cadastrar("um@exemplo.com", "52998224725", "Primeira Loja");
            cadastrar("dois@exemplo.com", "11222333000181", "Segunda Loja");
            String admin = criarAdministrador();

            mockMvc.perform(post("/api/cadastro/moderacao/{id}/aprovar", primeira)
                    .header("Authorization", "Bearer " + admin));

            mockMvc.perform(get("/api/cadastro/moderacao/indicadores")
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.empreendedoresAprovados").value(1))
                    .andExpect(jsonPath("$.cadastrosPendentes").value(1))
                    .andExpect(jsonPath("$.aprovadosSemNenhumProduto").value(1))
                    .andExpect(jsonPath("$.aprovadosPorBairro['Vila Cardia']").value(1));
        }

        @Test
        @DisplayName("a auditoria registra quem aprovou o quê")
        void auditoriaRegistra() throws Exception {
            UUID empreendedorId = cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            String admin = criarAdministrador();

            mockMvc.perform(post("/api/cadastro/moderacao/{id}/aprovar", empreendedorId)
                    .header("Authorization", "Bearer " + admin));

            mockMvc.perform(get("/api/cadastro/moderacao/auditoria")
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudo[?(@.acao == 'cadastro_aprovado')]")
                            .isNotEmpty());
        }
    }

    // ------------------------------------------------------------ privacidade

    @Nested
    @DisplayName("privacidade e LGPD")
    class Privacidade {

        @Test
        @DisplayName("o titular baixa tudo que a plataforma guarda sobre ele")
        void exportaDados() throws Exception {
            cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            String token = entrar("lourdes@exemplo.com", "bolodefuba2026");

            mockMvc.perform(get("/api/cadastro/privacidade/meus-dados")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conta.email").value("lourdes@exemplo.com"))
                    .andExpect(jsonPath("$.negocio.nomeDoNegocio").value("Doces da Lourdes"))
                    .andExpect(jsonPath("$.negocio.documento").value("529.982.247-25"))
                    .andExpect(jsonPath("$.historico").isArray());
        }

        @Test
        @DisplayName("pedir exclusão tira a loja do ar na hora e abre a saga")
        void pedirExclusao() throws Exception {
            UUID empreendedorId = cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            String token = entrar("lourdes@exemplo.com", "bolodefuba2026");

            mockMvc.perform(delete("/api/cadastro/privacidade/minha-conta")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.protocolo").isNotEmpty())
                    .andExpect(jsonPath("$.prazoLimite").isNotEmpty());

            assertThat(empreendedores.findById(empreendedorId).orElseThrow().status())
                    .isEqualTo(StatusDoCadastro.EXCLUIDO);
            assertThat(pedidosDeExclusao.findByEmpreendedorId(empreendedorId)).isPresent();
        }

        @Test
        @DisplayName("a saga só conclui quando os três serviços confirmam")
        void sagaEsperaTodos() throws Exception {
            UUID empreendedorId = cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            String token = entrar("lourdes@exemplo.com", "bolodefuba2026");
            UUID usuarioId = usuarios.findByEmail("lourdes@exemplo.com").orElseThrow().id();

            mockMvc.perform(delete("/api/cadastro/privacidade/minha-conta")
                    .header("Authorization", "Bearer " + token));

            confirmar(empreendedorId, Participante.CATALOGO);
            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(pedidosDeExclusao.findByEmpreendedorId(empreendedorId).orElseThrow()
                            .confirmados()).contains(Participante.CATALOGO));

            assertThat(pedidosDeExclusao.findByEmpreendedorId(empreendedorId).orElseThrow()
                    .concluidoEm())
                    .as("com um serviço só confirmando, a saga continua aberta")
                    .isNull();

            confirmar(empreendedorId, Participante.BUSCA);
            confirmar(empreendedorId, Participante.NOTIFICACOES);

            await().atMost(ESPERA).untilAsserted(() -> {
                var pedido = pedidosDeExclusao.findByEmpreendedorId(empreendedorId).orElseThrow();
                assertThat(pedido.estaCompleto()).isTrue();
                assertThat(pedido.concluidoEm()).isNotNull();
            });

            var usuario = usuarios.findById(usuarioId).orElseThrow();
            assertThat(usuario.nome()).isEqualTo("Conta removida");
            assertThat(usuario.email()).doesNotContain("lourdes@exemplo.com");
            assertThat(usuario.ativo()).isFalse();

            var empreendedor = empreendedores.findById(empreendedorId).orElseThrow();
            assertThat(empreendedor.nomeDoNegocio()).isEqualTo("Cadastro removido");
            assertThat(empreendedor.documento()).isNotEqualTo("52998224725");
        }

        @Test
        @DisplayName("a mesma confirmação chegando duas vezes não quebra a saga")
        void confirmacaoRepetidaNaoQuebra() throws Exception {
            UUID empreendedorId = cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            String token = entrar("lourdes@exemplo.com", "bolodefuba2026");

            mockMvc.perform(delete("/api/cadastro/privacidade/minha-conta")
                    .header("Authorization", "Bearer " + token));

            var confirmacao = new ExpurgoConcluido(UUID.randomUUID(), UUID.randomUUID(),
                    Instant.now(), empreendedorId, Participante.CATALOGO, 3);
            String carga = json.writeValueAsString(confirmacao);

            kafka.send(Topicos.PRIVACIDADE, empreendedorId.toString(), carga);
            kafka.send(Topicos.PRIVACIDADE, empreendedorId.toString(), carga);

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(pedidosDeExclusao.findByEmpreendedorId(empreendedorId).orElseThrow()
                            .confirmados()).containsExactly(Participante.CATALOGO));

            assertThat(pedidosDeExclusao.findByEmpreendedorId(empreendedorId).orElseThrow()
                    .concluidoEm()).isNull();
        }

        @Test
        @DisplayName("pedir exclusão duas vezes é conflito")
        void exclusaoDuplicada() throws Exception {
            cadastrar("lourdes@exemplo.com", "52998224725", "Doces da Lourdes");
            String token = entrar("lourdes@exemplo.com", "bolodefuba2026");

            mockMvc.perform(delete("/api/cadastro/privacidade/minha-conta")
                    .header("Authorization", "Bearer " + token));

            mockMvc.perform(delete("/api/cadastro/privacidade/minha-conta")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isConflict());
        }

        private void confirmar(UUID empreendedorId, Participante participante) throws Exception {
            var confirmacao = new ExpurgoConcluido(UUID.randomUUID(), UUID.randomUUID(),
                    Instant.now(), empreendedorId, participante, 2);
            kafka.send(Topicos.PRIVACIDADE, empreendedorId.toString(),
                    json.writeValueAsString(confirmacao));
        }
    }

    // ------------------------------------------------------------ saude

    @Nested
    @DisplayName("saúde do serviço")
    class Saude {

        @Test
        @DisplayName("responde ao teste de saúde sem exigir login")
        void saudePublica() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @DisplayName("a documentação da API está no ar")
        void documentacaoNoAr() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paths['/api/cadastro/empreendedores']").exists());
        }

        @Test
        @DisplayName("toda resposta carrega a correlação para rastrear entre serviços")
        void correlacaoNaResposta() throws Exception {
            mockMvc.perform(get("/api/cadastro/bairros"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .header().exists("X-Correlacao"));
        }
    }
}
