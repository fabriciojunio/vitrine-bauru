package br.com.vitrinebauru.catalogo;

import br.com.vitrinebauru.catalogo.dominio.EmpreendedorConhecido;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.EmpreendedorConhecidoRepository;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.ImagemRepository;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.ProdutoRepository;
import br.com.vitrinebauru.contratos.CadastroAprovado;
import br.com.vitrinebauru.contratos.EmpreendedorSuspenso;
import br.com.vitrinebauru.contratos.ExclusaoSolicitada;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.outbox.OutboxRepository;
import br.com.vitrinebauru.plataforma.seguranca.EmissorDeToken;
import br.com.vitrinebauru.plataforma.seguranca.Papel;
import br.com.vitrinebauru.plataforma.seguranca.UsuarioAutenticado;
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
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catalogo inteiro no ar, com banco e broker de verdade.
 *
 * <p>Este teste tambem cobre a parte mais facil de errar num sistema de
 * eventos: o catalogo so aceita produto de empreendedor que ele aprendeu por
 * evento. O teste publica o evento no topico e espera o servico reagir, em vez
 * de gravar a tabela na mao, porque e assim que acontece em producao.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EmbeddedKafka(
        partitions = 1,
        topics = {Topicos.EMPREENDEDORES, Topicos.CATALOGO, Topicos.CONTATOS, Topicos.PRIVACIDADE,
                Topicos.EMPREENDEDORES + ".dlq", Topicos.CATALOGO + ".dlq",
                Topicos.CONTATOS + ".dlq", Topicos.PRIVACIDADE + ".dlq"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
@DisplayName("Fluxo do catálogo, de ponta a ponta")
class FluxoDoCatalogoTest {

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
    private EmissorDeToken emissor;
    @Autowired
    private KafkaTemplate<String, String> kafka;
    @Autowired
    private ProdutoRepository produtos;
    @Autowired
    private ImagemRepository imagens;
    @Autowired
    private EmpreendedorConhecidoRepository conhecidos;
    @Autowired
    private OutboxRepository outbox;

    private UUID loja;
    private String token;

    @BeforeEach
    void preparar() {
        produtos.deleteAll();
        imagens.deleteAll();
        conhecidos.deleteAll();
        outbox.deleteAll();

        loja = UUID.randomUUID();
        token = emissor.emitir(new UsuarioAutenticado(
                UUID.randomUUID(), "lourdes@exemplo.com", Papel.EMPREENDEDOR, loja));
        conhecidos.save(new EmpreendedorConhecido(loja, "Doces da Lourdes", true, Instant.now()));
    }

    private JsonNode ler(MvcResult resultado) throws Exception {
        return json.readTree(resultado.getResponse().getContentAsString());
    }

    private UUID publicar(String nome, Long preco) throws Exception {
        String corpo = """
                {"nome":"%s","descricao":"Feito na hora","precoEmCentavos":%s,"categoria":"Alimentação"}
                """.formatted(nome, preco == null ? "null" : preco);

        MvcResult resultado = mockMvc.perform(post("/api/catalogo/meus-produtos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(ler(resultado).get("id").asText());
    }

    private static byte[] jpegDeTeste() {
        byte[] arquivo = new byte[512];
        arquivo[0] = (byte) 0xFF;
        arquivo[1] = (byte) 0xD8;
        arquivo[2] = (byte) 0xFF;
        arquivo[3] = (byte) 0xE0;
        return arquivo;
    }

    @Nested
    @DisplayName("publicar produto")
    class Publicar {

        @Test
        @DisplayName("publica e gera o evento que a busca vai consumir")
        void publicaEGeraEvento() throws Exception {
            UUID produtoId = publicar("Bolo de pote", 1200L);

            assertThat(produtos.findById(produtoId)).isPresent();

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(outbox.findAll())
                            .anyMatch(mensagem -> mensagem.tipo().equals("ProdutoPublicado")
                                    && mensagem.foiPublicada()));
        }

        @Test
        @DisplayName("aceita produto sem preço e mostra sob consulta")
        void semPreco() throws Exception {
            MvcResult resultado = mockMvc.perform(post("/api/catalogo/meus-produtos")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nome":"Móvel sob medida","descricao":"Conforme o projeto",
                                     "precoEmCentavos":null,"categoria":"Casa e construção"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.precoFormatado").value("Sob consulta"))
                    .andReturn();

            assertThat(ler(resultado).get("precoEmCentavos").isNull()).isTrue();
        }

        @Test
        @DisplayName("recusa preço negativo")
        void recusaPrecoNegativo() throws Exception {
            mockMvc.perform(post("/api/catalogo/meus-produtos")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nome":"Bolo","descricao":"x","precoEmCentavos":-100,
                                     "categoria":"Alimentação"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("recusa categoria que não existe")
        void recusaCategoriaInventada() throws Exception {
            mockMvc.perform(post("/api/catalogo/meus-produtos")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nome":"Bolo","descricao":"x","precoEmCentavos":100,
                                     "categoria":"Mineração"}
                                    """))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("recusa produto sem nome, apontando o campo")
        void recusaSemNome() throws Exception {
            mockMvc.perform(post("/api/catalogo/meus-produtos")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"descricao\":\"x\",\"categoria\":\"Alimentação\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.nome").exists());
        }

        @Test
        @DisplayName("limpa marcação HTML da descrição")
        void limpaHtml() throws Exception {
            MvcResult resultado = mockMvc.perform(post("/api/catalogo/meus-produtos")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nome":"Bolo","descricao":"<img src=x onerror=alert(1)>caseiro",
                                     "precoEmCentavos":100,"categoria":"Alimentação"}
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn();

            assertThat(ler(resultado).get("descricao").asText())
                    .doesNotContain("onerror")
                    .contains("caseiro");
        }

        @Test
        @DisplayName("sem token não publica")
        void semToken() throws Exception {
            mockMvc.perform(post("/api/catalogo/meus-produtos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"Bolo\",\"categoria\":\"Alimentação\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("loja desconhecida do catálogo recebe explicação, e não erro genérico")
        void lojaDesconhecida() throws Exception {
            String tokenDeOutraLoja = emissor.emitir(new UsuarioAutenticado(
                    UUID.randomUUID(), "outro@exemplo.com", Papel.EMPREENDEDOR, UUID.randomUUID()));

            mockMvc.perform(post("/api/catalogo/meus-produtos")
                            .header("Authorization", "Bearer " + tokenDeOutraLoja)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nome":"Bolo","descricao":"x","precoEmCentavos":100,
                                     "categoria":"Alimentação"}
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail")
                            .value(org.hamcrest.Matchers.containsString("ainda não chegou")));
        }
    }

    @Nested
    @DisplayName("cuidar do catálogo")
    class Cuidar {

        @Test
        @DisplayName("altera o produto e avisa o resto do sistema")
        void altera() throws Exception {
            UUID produtoId = publicar("Bolo de pote", 1200L);

            mockMvc.perform(put("/api/catalogo/meus-produtos/{id}", produtoId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nome":"Bolo de pote grande","descricao":"Agora com 350ml",
                                     "precoEmCentavos":1800,"categoria":"Alimentação"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value("Bolo de pote grande"))
                    .andExpect(jsonPath("$.precoFormatado").value("R$ 18,00"));

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(outbox.findAll())
                            .anyMatch(mensagem -> mensagem.tipo().equals("ProdutoAtualizado")));
        }

        @Test
        @DisplayName("marca como esgotado sem apagar")
        void marcaEsgotado() throws Exception {
            UUID produtoId = publicar("Bolo de pote", 1200L);

            mockMvc.perform(put("/api/catalogo/meus-produtos/{id}/disponibilidade", produtoId)
                            .header("Authorization", "Bearer " + token)
                            .param("disponivel", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.disponivel").value(false));

            assertThat(produtos.findById(produtoId)).isPresent();
        }

        @Test
        @DisplayName("retira do catálogo e some da listagem do painel")
        void retira() throws Exception {
            UUID produtoId = publicar("Bolo de pote", 1200L);

            mockMvc.perform(delete("/api/catalogo/meus-produtos/{id}", produtoId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/catalogo/meus-produtos")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.total").value(0));
        }

        @Test
        @DisplayName("não deixa mexer no produto de outra loja")
        void naoMexeNoProdutoDosOutros() throws Exception {
            UUID produtoId = publicar("Bolo de pote", 1200L);

            UUID outraLoja = UUID.randomUUID();
            conhecidos.save(new EmpreendedorConhecido(outraLoja, "Outra Loja", true, Instant.now()));
            String tokenDoVizinho = emissor.emitir(new UsuarioAutenticado(
                    UUID.randomUUID(), "vizinho@exemplo.com", Papel.EMPREENDEDOR, outraLoja));

            mockMvc.perform(delete("/api/catalogo/meus-produtos/{id}", produtoId)
                            .header("Authorization", "Bearer " + tokenDoVizinho))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail")
                            .value(org.hamcrest.Matchers.containsString("outra loja")));
        }

        @Test
        @DisplayName("lista só os produtos da própria loja")
        void listaSoOsProprios() throws Exception {
            publicar("Bolo de pote", 1200L);
            publicar("Brigadeiro", 300L);

            UUID outraLoja = UUID.randomUUID();
            conhecidos.save(new EmpreendedorConhecido(outraLoja, "Outra Loja", true, Instant.now()));
            String tokenDoVizinho = emissor.emitir(new UsuarioAutenticado(
                    UUID.randomUUID(), "vizinho@exemplo.com", Papel.EMPREENDEDOR, outraLoja));

            mockMvc.perform(get("/api/catalogo/meus-produtos")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.total").value(2));

            mockMvc.perform(get("/api/catalogo/meus-produtos")
                            .header("Authorization", "Bearer " + tokenDoVizinho))
                    .andExpect(jsonPath("$.total").value(0));
        }
    }

    @Nested
    @DisplayName("foto do produto")
    class Foto {

        @Test
        @DisplayName("envia a foto e ela fica acessível sem login")
        void enviaEBaixa() throws Exception {
            UUID produtoId = publicar("Bolo de pote", 1200L);
            var arquivo = new MockMultipartFile("arquivo", "bolo.jpg", "image/jpeg", jpegDeTeste());

            MvcResult resultado = mockMvc.perform(multipart(
                            "/api/catalogo/meus-produtos/{id}/imagem", produtoId)
                            .file(arquivo)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();

            String endereco = ler(resultado).get("imagemUrl").asText();

            mockMvc.perform(get(endereco))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "image/jpeg"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }

        @Test
        @DisplayName("recusa arquivo que não é imagem, mesmo com nome de foto")
        void recusaArquivoQueNaoEImagem() throws Exception {
            UUID produtoId = publicar("Bolo de pote", 1200L);
            var falsa = new MockMultipartFile("arquivo", "foto.jpg", "image/jpeg",
                    "<html><script>alert(1)</script></html>".getBytes());

            mockMvc.perform(multipart("/api/catalogo/meus-produtos/{id}/imagem", produtoId)
                            .file(falsa)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail")
                            .value(org.hamcrest.Matchers.containsString("não é uma imagem")));
        }

        @Test
        @DisplayName("imagem inexistente responde 404 em português")
        void imagemInexistente() throws Exception {
            mockMvc.perform(get("/api/catalogo/imagens/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Imagem não encontrada."));
        }
    }

    @Nested
    @DisplayName("reação a eventos do cadastro")
    class ReacaoAEventos {

        @Test
        @DisplayName("aprende um empreendedor novo pelo evento de aprovação")
        void aprendePeloEvento() throws Exception {
            UUID nova = UUID.randomUUID();
            var aprovado = new CadastroAprovado(UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                    nova, UUID.randomUUID(), "Marcenaria Irmãos Pereira",
                    "pereira@exemplo.com", "Antônio");

            kafka.send(Topicos.EMPREENDEDORES, nova.toString(), json.writeValueAsString(aprovado));

            await().atMost(ESPERA).untilAsserted(() -> {
                var conhecido = conhecidos.findById(nova);
                assertThat(conhecido).isPresent();
                assertThat(conhecido.get().podePublicar()).isTrue();
                assertThat(conhecido.get().nomeDoNegocio()).isEqualTo("Marcenaria Irmãos Pereira");
            });
        }

        @Test
        @DisplayName("loja suspensa perde o direito de publicar, e os produtos ficam")
        void suspensaNaoPublica() throws Exception {
            publicar("Bolo de pote", 1200L);

            var suspenso = new EmpreendedorSuspenso(UUID.randomUUID(), UUID.randomUUID(),
                    Instant.now(), loja, UUID.randomUUID(), "Denúncia em análise",
                    "Doces da Lourdes", "lourdes@exemplo.com", "Lourdes");

            kafka.send(Topicos.EMPREENDEDORES, loja.toString(), json.writeValueAsString(suspenso));

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(conhecidos.findById(loja).orElseThrow().podePublicar()).isFalse());

            mockMvc.perform(post("/api/catalogo/meus-produtos")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nome":"Outro bolo","descricao":"x","precoEmCentavos":100,
                                     "categoria":"Alimentação"}
                                    """))
                    .andExpect(status().isForbidden());

            assertThat(produtos.countByEmpreendedorIdAndRetiradoEmIsNull(loja)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("expurgo de dados (LGPD)")
    class Expurgo {

        @Test
        @DisplayName("apaga produtos e fotos, e confirma para o coordenador")
        void apagaEConfirma() throws Exception {
            UUID produtoId = publicar("Bolo de pote", 1200L);
            var arquivo = new MockMultipartFile("arquivo", "bolo.jpg", "image/jpeg", jpegDeTeste());
            mockMvc.perform(multipart("/api/catalogo/meus-produtos/{id}/imagem", produtoId)
                    .file(arquivo).header("Authorization", "Bearer " + token));

            var pedido = new ExclusaoSolicitada(UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                    loja, UUID.randomUUID(), Instant.now().plusSeconds(1_296_000));

            kafka.send(Topicos.PRIVACIDADE, loja.toString(), json.writeValueAsString(pedido));

            await().atMost(ESPERA).untilAsserted(() -> {
                assertThat(produtos.findByEmpreendedorIdAndRetiradoEmIsNull(loja)).isEmpty();
                assertThat(imagens.findAll()).isEmpty();
                assertThat(conhecidos.findById(loja)).isEmpty();
                assertThat(outbox.findAll())
                        .anyMatch(mensagem -> mensagem.tipo().equals("ExpurgoConcluido"));
            });
        }

        @Test
        @DisplayName("o mesmo pedido chegando duas vezes não quebra nada")
        void pedidoRepetido() throws Exception {
            publicar("Bolo de pote", 1200L);

            var pedido = new ExclusaoSolicitada(UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                    loja, UUID.randomUUID(), Instant.now().plusSeconds(1_296_000));
            String carga = json.writeValueAsString(pedido);

            kafka.send(Topicos.PRIVACIDADE, loja.toString(), carga);
            kafka.send(Topicos.PRIVACIDADE, loja.toString(), carga);

            await().atMost(ESPERA).untilAsserted(() ->
                    assertThat(produtos.findByEmpreendedorIdAndRetiradoEmIsNull(loja)).isEmpty());
        }
    }

    @Nested
    @DisplayName("parte pública")
    class PartePublica {

        @Test
        @DisplayName("a lista de categorias é pública e vem ordenada")
        void categoriasPublicas() throws Exception {
            mockMvc.perform(get("/api/catalogo/categorias"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].nome").value("Alimentação"))
                    .andExpect(jsonPath("$.length()").value(12));
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
