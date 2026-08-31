package br.com.vitrinebauru.busca;

import br.com.vitrinebauru.busca.infraestrutura.persistencia.LojaRepository;
import br.com.vitrinebauru.busca.infraestrutura.persistencia.ProdutoDaVitrineRepository;
import br.com.vitrinebauru.contratos.CadastroAprovado;
import br.com.vitrinebauru.contratos.EmpreendedorCadastrado;
import br.com.vitrinebauru.contratos.EmpreendedorReativado;
import br.com.vitrinebauru.contratos.EmpreendedorSuspenso;
import br.com.vitrinebauru.contratos.ExclusaoSolicitada;
import br.com.vitrinebauru.contratos.ProdutoPublicado;
import br.com.vitrinebauru.contratos.ProdutoRetirado;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A vitrine montada do jeito que ela se monta em producao: so por evento.
 *
 * <p>Nenhum teste daqui grava na tabela de projecao. Tudo entra pelo topico,
 * como entraria vindo do cadastro e do catalogo. E o unico jeito de provar que
 * a vitrine que o consumidor ve corresponde ao que a SEDECON aprovou.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EmbeddedKafka(
        partitions = 1,
        topics = {Topicos.EMPREENDEDORES, Topicos.CATALOGO, Topicos.CONTATOS, Topicos.PRIVACIDADE,
                Topicos.EMPREENDEDORES + ".dlq", Topicos.CATALOGO + ".dlq",
                Topicos.CONTATOS + ".dlq", Topicos.PRIVACIDADE + ".dlq"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
@DisplayName("Vitrine pública")
class VitrinePublicaTest {

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
        propriedades.add("vitrine.limite.ativo", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private KafkaTemplate<String, String> kafka;
    @Autowired
    private LojaRepository lojas;
    @Autowired
    private ProdutoDaVitrineRepository produtos;
    @Autowired
    private OutboxRepository outbox;

    private UUID docesDaLourdes;
    private UUID acaiDoJoao;

    @BeforeEach
    void preparar() {
        produtos.deleteAll();
        lojas.deleteAll();
        outbox.deleteAll();

        docesDaLourdes = UUID.randomUUID();
        acaiDoJoao = UUID.randomUUID();
    }

    private void publicar(String topico, Object evento, UUID chave) throws Exception {
        kafka.send(topico, chave.toString(), json.writeValueAsString(evento));
    }

    private void cadastrarLoja(UUID id, String nome, String apelido, String descricao,
                               String categoria, String bairro, String telefone) throws Exception {
        publicar(Topicos.EMPREENDEDORES, new EmpreendedorCadastrado(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now(), id, UUID.randomUUID(),
                nome, apelido, descricao, categoria, bairro, telefone,
                "52998224725", "dono@exemplo.com", "Dono"), id);
    }

    private void aprovarLoja(UUID id, String nome) throws Exception {
        publicar(Topicos.EMPREENDEDORES, new CadastroAprovado(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now(), id, UUID.randomUUID(),
                nome, "dono@exemplo.com", "Dono"), id);
    }

    private UUID publicarProduto(UUID loja, String nome, String descricao, Long preco,
                                 String categoria) throws Exception {
        UUID produtoId = UUID.randomUUID();
        publicar(Topicos.CATALOGO, new ProdutoPublicado(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now(), produtoId, loja,
                nome, descricao, preco, UUID.randomUUID(), categoria, null, true), loja);
        return produtoId;
    }

    /** Monta uma vitrine pequena e espera ela ficar pronta. */
    private void montarVitrine() throws Exception {
        cadastrarLoja(docesDaLourdes, "Doces da Lourdes", "doces-da-lourdes",
                "Bolo de pote e salgado de festa", "Alimentação", "Vila Cardia", "14997010101");
        cadastrarLoja(acaiDoJoao, "Açaí do João", "acai-do-joao",
                "Açaí no copo e na tigela", "Alimentação", "Vila Falcão", "14997020202");

        aprovarLoja(docesDaLourdes, "Doces da Lourdes");
        aprovarLoja(acaiDoJoao, "Açaí do João");

        publicarProduto(docesDaLourdes, "Bolo de pote", "Massa de chocolate", 1200L, "Alimentação");
        publicarProduto(docesDaLourdes, "Torta salgada", "Serve 8 pessoas", 6500L, "Alimentação");
        publicarProduto(acaiDoJoao, "Açaí 500ml", "Com granola e banana", 1800L, "Alimentação");

        await().atMost(ESPERA).untilAsserted(() -> {
            assertThat(lojas.countByVisivelIsTrue()).isEqualTo(2);
            assertThat(produtos.countByVisivelIsTrueAndDisponivelIsTrue()).isEqualTo(3);
        });
    }

    @Nested
    @DisplayName("montagem por evento")
    class MontagemPorEvento {

        @Test
        @DisplayName("loja cadastrada não aparece antes de a SEDECON aprovar")
        void naoApareceAntesDeAprovar() throws Exception {
            cadastrarLoja(docesDaLourdes, "Doces da Lourdes", "doces-da-lourdes",
                    "Bolo de pote", "Alimentação", "Vila Cardia", "14997010101");

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(lojas.findById(docesDaLourdes)).isPresent());

            assertThat(lojas.countByVisivelIsTrue()).isZero();

            mockMvc.perform(get("/api/busca/lojas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0));
        }

        @Test
        @DisplayName("aprovação coloca a loja no ar")
        void aprovacaoColocaNoAr() throws Exception {
            cadastrarLoja(docesDaLourdes, "Doces da Lourdes", "doces-da-lourdes",
                    "Bolo de pote", "Alimentação", "Vila Cardia", "14997010101");
            aprovarLoja(docesDaLourdes, "Doces da Lourdes");

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(lojas.countByVisivelIsTrue()).isEqualTo(1));

            mockMvc.perform(get("/api/busca/lojas"))
                    .andExpect(jsonPath("$.conteudo[0].nomeDoNegocio").value("Doces da Lourdes"))
                    .andExpect(jsonPath("$.conteudo[0].telefoneWhatsapp").value("(14) 99701-0101"));
        }

        @Test
        @DisplayName("suspensão tira a loja e os produtos dela do ar de uma vez")
        void suspensaoTiraTudo() throws Exception {
            montarVitrine();

            publicar(Topicos.EMPREENDEDORES, new EmpreendedorSuspenso(
                    UUID.randomUUID(), UUID.randomUUID(), Instant.now(), docesDaLourdes,
                    UUID.randomUUID(), "Denúncia em análise", "Doces da Lourdes",
                    "dono@exemplo.com", "Dono"), docesDaLourdes);

            await().atMost(ESPERA).untilAsserted(() -> {
                assertThat(lojas.countByVisivelIsTrue()).isEqualTo(1);
                assertThat(produtos.countByVisivelIsTrueAndDisponivelIsTrue()).isEqualTo(1);
            });

            mockMvc.perform(get("/api/busca/produtos").param("termo", "bolo"))
                    .andExpect(jsonPath("$.total").value(0));
        }

        @Test
        @DisplayName("reativação devolve loja e produtos à vitrine")
        void reativacaoDevolve() throws Exception {
            montarVitrine();

            publicar(Topicos.EMPREENDEDORES, new EmpreendedorSuspenso(
                    UUID.randomUUID(), UUID.randomUUID(), Instant.now(), docesDaLourdes,
                    UUID.randomUUID(), "Denúncia em análise", "Doces da Lourdes",
                    "dono@exemplo.com", "Dono"), docesDaLourdes);

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(lojas.countByVisivelIsTrue()).isEqualTo(1));

            publicar(Topicos.EMPREENDEDORES, new EmpreendedorReativado(
                    UUID.randomUUID(), UUID.randomUUID(), Instant.now(), docesDaLourdes,
                    UUID.randomUUID(), "Doces da Lourdes", "dono@exemplo.com", "Dono"),
                    docesDaLourdes);

            await().atMost(ESPERA).untilAsserted(() -> {
                assertThat(lojas.countByVisivelIsTrue()).isEqualTo(2);
                assertThat(produtos.countByVisivelIsTrueAndDisponivelIsTrue()).isEqualTo(3);
            });
        }

        @Test
        @DisplayName("produto que chega antes da loja espera e aparece quando ela chega")
        void produtoAntesDaLoja() throws Exception {
            publicarProduto(docesDaLourdes, "Bolo de pote", "Massa de chocolate", 1200L, "Alimentação");

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(produtos.findByEmpreendedorId(docesDaLourdes)).hasSize(1));

            assertThat(produtos.countByVisivelIsTrueAndDisponivelIsTrue())
                    .as("sem a loja, o produto fica invisível em vez de sumir")
                    .isZero();

            cadastrarLoja(docesDaLourdes, "Doces da Lourdes", "doces-da-lourdes",
                    "Bolo de pote", "Alimentação", "Vila Cardia", "14997010101");
            aprovarLoja(docesDaLourdes, "Doces da Lourdes");

            await().atMost(ESPERA).untilAsserted(() -> {
                assertThat(produtos.countByVisivelIsTrueAndDisponivelIsTrue()).isEqualTo(1);
                assertThat(produtos.findByEmpreendedorId(docesDaLourdes).getFirst().lojaNome())
                        .isEqualTo("Doces da Lourdes");
            });
        }

        @Test
        @DisplayName("produto retirado some da vitrine")
        void produtoRetiradoSome() throws Exception {
            montarVitrine();
            UUID produtoId = produtos.findByEmpreendedorId(docesDaLourdes).getFirst().id();

            publicar(Topicos.CATALOGO, new ProdutoRetirado(
                    UUID.randomUUID(), UUID.randomUUID(), Instant.now(), produtoId, docesDaLourdes),
                    docesDaLourdes);

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(produtos.countByVisivelIsTrueAndDisponivelIsTrue()).isEqualTo(2));
        }

        @Test
        @DisplayName("o mesmo evento chegando duas vezes não duplica nada")
        void eventoRepetidoNaoDuplica() throws Exception {
            var evento = new EmpreendedorCadastrado(
                    UUID.randomUUID(), UUID.randomUUID(), Instant.now(), docesDaLourdes,
                    UUID.randomUUID(), "Doces da Lourdes", "doces-da-lourdes", "Bolo",
                    "Alimentação", "Vila Cardia", "14997010101", "52998224725",
                    "dono@exemplo.com", "Dono");

            String carga = json.writeValueAsString(evento);
            kafka.send(Topicos.EMPREENDEDORES, docesDaLourdes.toString(), carga);
            kafka.send(Topicos.EMPREENDEDORES, docesDaLourdes.toString(), carga);
            kafka.send(Topicos.EMPREENDEDORES, docesDaLourdes.toString(), carga);

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(lojas.findById(docesDaLourdes)).isPresent());

            assertThat(lojas.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("busca do consumidor")
    class BuscaDoConsumidor {

        @ParameterizedTest(name = "procurar por \"{0}\" encontra o açaí")
        @ValueSource(strings = {"acai", "açai", "Açaí", "AÇAÍ", "acaí", "acai 500"})
        @DisplayName("acha com acento, sem acento e com pedaço da palavra")
        void achaComOuSemAcento(String termo) throws Exception {
            montarVitrine();

            // A asserção olha o conjunto, e não a primeira posição: termo curto
            // casa com mais de um produto (a própria palavra "alimentacao"
            // contém "aca"), e ordenar por relevância é assunto de outra etapa.
            mockMvc.perform(get("/api/busca/produtos").param("termo", termo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudo[*].nome",
                            org.hamcrest.Matchers.hasItem(
                                    org.hamcrest.Matchers.containsString("Açaí"))));
        }

        @Test
        @DisplayName("acha produto pelo nome da loja")
        void achaPeloNomeDaLoja() throws Exception {
            montarVitrine();

            mockMvc.perform(get("/api/busca/produtos").param("termo", "lourdes"))
                    .andExpect(jsonPath("$.total").value(2));
        }

        @Test
        @DisplayName("acha produto pela descrição")
        void achaPelaDescricao() throws Exception {
            montarVitrine();

            mockMvc.perform(get("/api/busca/produtos").param("termo", "granola"))
                    .andExpect(jsonPath("$.total").value(1));
        }

        @Test
        @DisplayName("filtra por bairro")
        void filtraPorBairro() throws Exception {
            montarVitrine();

            mockMvc.perform(get("/api/busca/produtos").param("bairro", "Vila Cardia"))
                    .andExpect(jsonPath("$.total").value(2));

            mockMvc.perform(get("/api/busca/produtos").param("bairro", "Vila Falcão"))
                    .andExpect(jsonPath("$.total").value(1));
        }

        @Test
        @DisplayName("aceita bairro digitado sem acento")
        void bairroSemAcento() throws Exception {
            montarVitrine();

            mockMvc.perform(get("/api/busca/produtos").param("bairro", "vila falcao"))
                    .andExpect(jsonPath("$.total").value(1));
        }

        @Test
        @DisplayName("recusa bairro que não é de Bauru, em vez de devolver lista vazia")
        void recusaBairroDeFora() throws Exception {
            mockMvc.perform(get("/api/busca/produtos").param("bairro", "Copacabana"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail")
                            .value(org.hamcrest.Matchers.containsString("Bauru")));
        }

        @Test
        @DisplayName("filtra por preço máximo e ignora o que é sob consulta")
        void filtraPorPreco() throws Exception {
            montarVitrine();
            publicarProduto(docesDaLourdes, "Bolo de casamento", "Sob encomenda", null, "Alimentação");

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(produtos.countByVisivelIsTrueAndDisponivelIsTrue()).isEqualTo(4));

            mockMvc.perform(get("/api/busca/produtos").param("precoMaximoEmCentavos", "1500"))
                    .andExpect(jsonPath("$.total").value(1));
        }

        @Test
        @DisplayName("combina palavra, bairro e categoria")
        void combinaFiltros() throws Exception {
            montarVitrine();

            mockMvc.perform(get("/api/busca/produtos")
                            .param("termo", "bolo")
                            .param("bairro", "Vila Cardia")
                            .param("categoria", "Alimentação"))
                    .andExpect(jsonPath("$.total").value(1));

            mockMvc.perform(get("/api/busca/produtos")
                            .param("termo", "bolo")
                            .param("bairro", "Vila Falcão"))
                    .andExpect(jsonPath("$.total").value(0));
        }

        @Test
        @DisplayName("sem filtro nenhum, mostra a vitrine inteira")
        void semFiltroMostraTudo() throws Exception {
            montarVitrine();

            mockMvc.perform(get("/api/busca/produtos"))
                    .andExpect(jsonPath("$.total").value(3));
        }

        @Test
        @DisplayName("termo de uma letra é ignorado, para não varrer a base a cada tecla")
        void termoCurtoEIgnorado() throws Exception {
            montarVitrine();

            mockMvc.perform(get("/api/busca/produtos").param("termo", "a"))
                    .andExpect(jsonPath("$.total").value(3));
        }

        @Test
        @DisplayName("preço aparece formatado, e sem preço vira sob consulta")
        void precoFormatado() throws Exception {
            montarVitrine();

            mockMvc.perform(get("/api/busca/produtos").param("termo", "bolo de pote"))
                    .andExpect(jsonPath("$.conteudo[0].precoFormatado").value("R$ 12,00"));

            publicarProduto(docesDaLourdes, "Bolo de casamento", "Sob encomenda", null, "Alimentação");
            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(produtos.countByVisivelIsTrueAndDisponivelIsTrue()).isEqualTo(4));

            mockMvc.perform(get("/api/busca/produtos").param("termo", "casamento"))
                    .andExpect(jsonPath("$.conteudo[0].precoFormatado").value("Sob consulta"));
        }

        @Test
        @DisplayName("a página tem teto de tamanho")
        void paginaTemTeto() throws Exception {
            montarVitrine();

            mockMvc.perform(get("/api/busca/produtos").param("tamanho", "10000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tamanho").value(48));
        }
    }

    @Nested
    @DisplayName("página da loja")
    class PaginaDaLoja {

        @Test
        @DisplayName("mostra a loja e o catálogo dela pelo endereço amigável")
        void mostraLojaEProdutos() throws Exception {
            montarVitrine();

            mockMvc.perform(get("/api/busca/lojas/{apelido}", "doces-da-lourdes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.loja.nomeDoNegocio").value("Doces da Lourdes"))
                    .andExpect(jsonPath("$.loja.bairro").value("Vila Cardia"))
                    .andExpect(jsonPath("$.produtos.length()").value(2));
        }

        @Test
        @DisplayName("loja que não existe responde 404 em português")
        void lojaInexistente() throws Exception {
            mockMvc.perform(get("/api/busca/lojas/{apelido}", "loja-que-nao-existe"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Essa loja não está disponível."));
        }

        @Test
        @DisplayName("loja suspensa some da página pública")
        void lojaSuspensaSome() throws Exception {
            montarVitrine();

            publicar(Topicos.EMPREENDEDORES, new EmpreendedorSuspenso(
                    UUID.randomUUID(), UUID.randomUUID(), Instant.now(), docesDaLourdes,
                    UUID.randomUUID(), "Denúncia em análise", "Doces da Lourdes",
                    "dono@exemplo.com", "Dono"), docesDaLourdes);

            await().atMost(ESPERA).untilAsserted(() ->
                    mockMvc.perform(get("/api/busca/lojas/{apelido}", "doces-da-lourdes"))
                            .andExpect(status().isNotFound()));
        }

        @Test
        @DisplayName("o resumo traz os números e os filtros que existem de verdade")
        void resumo() throws Exception {
            montarVitrine();

            mockMvc.perform(get("/api/busca/resumo"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lojas").value(2))
                    .andExpect(jsonPath("$.produtos").value(3))
                    .andExpect(jsonPath("$.bairros").isArray())
                    .andExpect(jsonPath("$.categorias[0]").value("Alimentação"));
        }
    }

    @Nested
    @DisplayName("contato pelo WhatsApp")
    class Contato {

        @Test
        @DisplayName("monta o link com a mensagem pronta e registra o evento")
        void montaLinkERegistra() throws Exception {
            montarVitrine();

            mockMvc.perform(post("/api/busca/contatos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"empreendedorId":"%s","nomeDoProduto":"Bolo de pote",
                                     "canal":"WHATSAPP","origem":"PAGINA_DO_PRODUTO"}
                                    """.formatted(docesDaLourdes)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.linkDoWhatsapp")
                            .value(org.hamcrest.Matchers.startsWith("https://wa.me/5514997010101?text=")))
                    .andExpect(jsonPath("$.nomeDoNegocio").value("Doces da Lourdes"));

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(outbox.findAll())
                            .anyMatch(mensagem -> mensagem.tipo().equals("ContatoIniciado")
                                    && mensagem.foiPublicada()));
        }

        @Test
        @DisplayName("a mensagem vai com acento e escapada para a URL")
        void mensagemComAcento() throws Exception {
            montarVitrine();

            var resultado = mockMvc.perform(post("/api/busca/contatos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"empreendedorId":"%s","nomeDoProduto":"Açaí 500ml"}
                                    """.formatted(acaiDoJoao)))
                    .andExpect(status().isOk())
                    .andReturn();

            String link = json.readTree(resultado.getResponse().getContentAsString())
                    .get("linkDoWhatsapp").asText();

            assertThat(link).contains("A%C3%A7a%C3%AD");
            assertThat(link).doesNotContain(" ");
        }

        @Test
        @DisplayName("não registra contato para loja que não está no ar")
        void naoRegistraParaLojaForaDoAr() throws Exception {
            mockMvc.perform(post("/api/busca/contatos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"empreendedorId\":\"%s\"}".formatted(UUID.randomUUID())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("exige a loja no pedido")
        void exigeLoja() throws Exception {
            mockMvc.perform(post("/api/busca/contatos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nomeDoProduto\":\"Bolo\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.empreendedorId").exists());
        }
    }

    @Nested
    @DisplayName("exclusão de dados (LGPD)")
    class Exclusao {

        @Test
        @DisplayName("apaga a loja e os produtos da vitrine e confirma o expurgo")
        void apagaEConfirma() throws Exception {
            montarVitrine();

            publicar(Topicos.PRIVACIDADE, new ExclusaoSolicitada(
                    UUID.randomUUID(), UUID.randomUUID(), Instant.now(), docesDaLourdes,
                    UUID.randomUUID(), Instant.now().plusSeconds(1_296_000)), docesDaLourdes);

            await().atMost(ESPERA).untilAsserted(() -> {
                assertThat(lojas.findById(docesDaLourdes)).isEmpty();
                assertThat(produtos.findByEmpreendedorId(docesDaLourdes)).isEmpty();
                assertThat(outbox.findAll())
                        .anyMatch(mensagem -> mensagem.tipo().equals("ExpurgoConcluido"));
            });

            mockMvc.perform(get("/api/busca/lojas/{apelido}", "doces-da-lourdes"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("saúde e documentação")
    class SaudeEDocumentacao {

        @Test
        @DisplayName("a vitrine responde sem login")
        void semLogin() throws Exception {
            mockMvc.perform(get("/api/busca/produtos")).andExpect(status().isOk());
            mockMvc.perform(get("/api/busca/lojas")).andExpect(status().isOk());
            mockMvc.perform(get("/api/busca/resumo")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("responde ao teste de saúde")
        void saude() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }
}
