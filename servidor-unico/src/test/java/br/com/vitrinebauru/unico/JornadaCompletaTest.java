package br.com.vitrinebauru.unico;

import br.com.vitrinebauru.plataforma.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A jornada inteira, do cadastro ao pedido de exclusão, com os quatro módulos
 * no mesmo processo e sem broker nenhum.
 *
 * <p>É o teste mais importante do projeto. Os outros provam cada serviço; este
 * prova que eles conversam: o cadastro grava no outbox, o transporte no
 * processo entrega ao despachante, o catálogo e a busca reagem, e a vitrine
 * pública mostra o resultado. É exatamente o caminho que roda na demonstração
 * publicada.
 *
 * <p>Os testes rodam em ordem, de propósito. Não é teste de unidade
 * independente: é uma história, e cada passo depende do anterior, do mesmo
 * jeito que a vida real de um empreendedor na plataforma.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Jornada completa num processo só")
class JornadaCompletaTest {

    private static final Duration ESPERA = Duration.ofSeconds(30);
    private static EmbeddedPostgres postgres;

    /** Guardado entre os passos da história. */
    private static UUID empreendedorId;
    private static UUID produtoId;
    private static String tokenDoEmpreendedor;
    private static String tokenDoAdmin;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry propriedades) throws Exception {
        postgres = EmbeddedPostgres.builder().start();
        propriedades.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        propriedades.add("spring.datasource.username", () -> "postgres");
        propriedades.add("spring.datasource.password", () -> "");

        // O ponto do teste: nenhum broker no ar.
        propriedades.add("vitrine.mensageria.transporte", () -> "processo");
        propriedades.add("vitrine.outbox.intervalo-ms", () -> "100");
        propriedades.add("vitrine.limite.ativo", () -> "false");
        propriedades.add("vitrine.brasilapi.ativa", () -> "false");
        propriedades.add("vitrine.demo.ativo", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private OutboxRepository outbox;
    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder codificador;
    @Autowired
    private br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository usuarios;

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

    @Test
    @Order(1)
    @DisplayName("1. a vitrine começa vazia e responde mesmo assim")
    void vitrineComecaVazia() throws Exception {
        mockMvc.perform(get("/api/busca/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        mockMvc.perform(get("/api/busca/resumo"))
                .andExpect(jsonPath("$.lojas").value(0));
    }

    @Test
    @Order(2)
    @DisplayName("2. a empreendedora se cadastra e entra na fila da SEDECON")
    void empreendedoraSeCadastra() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/cadastro/empreendedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Maria de Lourdes Prado",
                                  "email": "lourdes@exemplo.com",
                                  "senha": "bolodefuba2026",
                                  "nomeDoNegocio": "Doces da Lourdes",
                                  "descricao": "Bolo de pote, brigadeiro e torta salgada por encomenda",
                                  "categoriaPrincipal": "Alimentação",
                                  "bairro": "Vila Cardia",
                                  "cep": "17011-066",
                                  "telefoneWhatsapp": "(14) 99712-3456",
                                  "documento": "529.982.247-25"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        empreendedorId = UUID.fromString(ler(resultado).get("empreendedorId").asText());
        tokenDoEmpreendedor = entrar("lourdes@exemplo.com", "bolodefuba2026");

        mockMvc.perform(get("/api/cadastro/minha-loja")
                        .header("Authorization", "Bearer " + tokenDoEmpreendedor))
                .andExpect(jsonPath("$.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.apareceNaVitrine").value(false));
    }

    @Test
    @Order(3)
    @DisplayName("3. o evento do cadastro atravessa os módulos sem broker nenhum")
    void eventoAtravessaOsModulos() {
        await().atMost(ESPERA).untilAsserted(() -> {
            assertThat(outbox.findAll())
                    .as("o evento precisa sair do outbox mesmo sem Kafka")
                    .anyMatch(mensagem -> mensagem.tipo().equals("EmpreendedorCadastrado")
                            && mensagem.foiPublicada());

            // O catálogo aprendeu que a loja existe, e a busca guardou a
            // projeção ainda invisível.
            mockMvc.perform(get("/api/busca/lojas"))
                    .andExpect(jsonPath("$.total").value(0));
        });
    }

    @Test
    @Order(4)
    @DisplayName("4. a empreendedora monta o catálogo antes mesmo da aprovação")
    void montaOCatalogoAntesDaAprovacao() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/catalogo/meus-produtos")
                        .header("Authorization", "Bearer " + tokenDoEmpreendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Bolo de pote","descricao":"Massa de chocolate com brigadeiro",
                                 "precoEmCentavos":1200,"categoria":"Alimentação"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        produtoId = UUID.fromString(ler(resultado).get("id").asText());

        mockMvc.perform(post("/api/catalogo/meus-produtos")
                        .header("Authorization", "Bearer " + tokenDoEmpreendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Torta salgada de frango","descricao":"Serve 8 pessoas",
                                 "precoEmCentavos":6500,"categoria":"Alimentação"}
                                """))
                .andExpect(status().isCreated());

        // Nada disso aparece na vitrine ainda: a loja não foi aprovada.
        mockMvc.perform(get("/api/busca/produtos"))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @Order(5)
    @DisplayName("5. a empreendedora manda a foto do produto")
    void mandaAFoto() throws Exception {
        byte[] jpeg = new byte[1024];
        jpeg[0] = (byte) 0xFF;
        jpeg[1] = (byte) 0xD8;
        jpeg[2] = (byte) 0xFF;
        jpeg[3] = (byte) 0xE0;

        MvcResult resultado = mockMvc.perform(multipart(
                        "/api/catalogo/meus-produtos/{id}/imagem", produtoId)
                        .file(new MockMultipartFile("arquivo", "bolo.jpg", "image/jpeg", jpeg))
                        .header("Authorization", "Bearer " + tokenDoEmpreendedor))
                .andExpect(status().isOk())
                .andReturn();

        String endereco = ler(resultado).get("imagemUrl").asText();

        mockMvc.perform(get(endereco))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Content-Type", "image/jpeg"));
    }

    @Test
    @Order(6)
    @DisplayName("6. a SEDECON encontra o cadastro na fila e aprova")
    void sedeconAprova() throws Exception {
        var admin = usuarios.save(br.com.vitrinebauru.cadastro.dominio.Usuario.novo(
                "Analista SEDECON", "analista@bauru.sp.gov.br",
                codificador.encode("moderacao2026"),
                br.com.vitrinebauru.plataforma.seguranca.Papel.ADMIN_SEDECON,
                java.time.Instant.now()));

        tokenDoAdmin = entrar(admin.email(), "moderacao2026");

        mockMvc.perform(get("/api/cadastro/moderacao/fila")
                        .header("Authorization", "Bearer " + tokenDoAdmin))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.conteudo[0].nomeDoNegocio").value("Doces da Lourdes"))
                .andExpect(jsonPath("$.conteudo[0].documento")
                        .value(org.hamcrest.Matchers.startsWith("***")));

        mockMvc.perform(post("/api/cadastro/moderacao/{id}/aprovar", empreendedorId)
                        .header("Authorization", "Bearer " + tokenDoAdmin))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(7)
    @DisplayName("7. a aprovação chega à vitrine e a loja aparece com o catálogo pronto")
    void aprovacaoChegaAVitrine() {
        await().atMost(ESPERA).untilAsserted(() -> {
            mockMvc.perform(get("/api/busca/produtos"))
                    .andExpect(jsonPath("$.total").value(2));

            mockMvc.perform(get("/api/busca/lojas/{apelido}", "doces-da-lourdes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.loja.bairro").value("Vila Cardia"))
                    .andExpect(jsonPath("$.produtos.length()").value(2));
        });
    }

    @Test
    @Order(8)
    @DisplayName("8. o consumidor procura por bolo, sem login, e encontra")
    void consumidorEncontra() throws Exception {
        mockMvc.perform(get("/api/busca/produtos").param("termo", "bolo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].nome").value("Bolo de pote"))
                .andExpect(jsonPath("$.conteudo[0].precoFormatado").value("R$ 12,00"))
                .andExpect(jsonPath("$.conteudo[0].lojaNome").value("Doces da Lourdes"))
                .andExpect(jsonPath("$.conteudo[0].bairro").value("Vila Cardia"));

        mockMvc.perform(get("/api/busca/produtos").param("bairro", "Vila Cardia"))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @Order(9)
    @DisplayName("9. o consumidor clica em falar no WhatsApp e o contato vira número")
    void contatoViraNumero() throws Exception {
        mockMvc.perform(post("/api/busca/contatos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"empreendedorId":"%s","produtoId":"%s","nomeDoProduto":"Bolo de pote",
                                 "canal":"WHATSAPP","origem":"PAGINA_DO_PRODUTO"}
                                """.formatted(empreendedorId, produtoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkDoWhatsapp")
                        .value(org.hamcrest.Matchers.startsWith("https://wa.me/5514997123456")));

        await().atMost(ESPERA).untilAsserted(() ->
                mockMvc.perform(get("/api/cadastro/minha-loja/indicadores")
                                .header("Authorization", "Bearer " + tokenDoEmpreendedor))
                        .andExpect(jsonPath("$.contatosNoTotal").value(1))
                        .andExpect(jsonPath("$.produtos").value(2)));
    }

    @Test
    @Order(10)
    @DisplayName("10. o painel da SEDECON mostra o impacto da plataforma")
    void painelMostraImpacto() throws Exception {
        mockMvc.perform(get("/api/cadastro/moderacao/indicadores")
                        .header("Authorization", "Bearer " + tokenDoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empreendedoresAprovados").value(1))
                .andExpect(jsonPath("$.cadastrosPendentes").value(0))
                .andExpect(jsonPath("$.produtosPublicados").value(2))
                .andExpect(jsonPath("$.contatosNoTotal").value(1))
                .andExpect(jsonPath("$.aprovadosSemNenhumProduto").value(0))
                .andExpect(jsonPath("$.aprovadosPorBairro['Vila Cardia']").value(1))
                .andExpect(jsonPath("$.maisProcurados[0].nomeDoNegocio").value("Doces da Lourdes"));
    }

    @Test
    @Order(11)
    @DisplayName("11. a suspensão tira a loja do ar na hora")
    void suspensaoTiraDoAr() throws Exception {
        mockMvc.perform(post("/api/cadastro/moderacao/{id}/suspender", empreendedorId)
                        .header("Authorization", "Bearer " + tokenDoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Denúncia de propaganda enganosa em análise\"}"))
                .andExpect(status().isNoContent());

        await().atMost(ESPERA).untilAsserted(() ->
                mockMvc.perform(get("/api/busca/produtos"))
                        .andExpect(jsonPath("$.total").value(0)));

        mockMvc.perform(get("/api/busca/lojas/{apelido}", "doces-da-lourdes"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(12)
    @DisplayName("12. a reativação devolve tudo à vitrine")
    void reativacaoDevolve() throws Exception {
        mockMvc.perform(post("/api/cadastro/moderacao/{id}/reativar", empreendedorId)
                        .header("Authorization", "Bearer " + tokenDoAdmin))
                .andExpect(status().isNoContent());

        await().atMost(ESPERA).untilAsserted(() ->
                mockMvc.perform(get("/api/busca/produtos"))
                        .andExpect(jsonPath("$.total").value(2)));
    }

    @Test
    @Order(13)
    @DisplayName("13. a empreendedora baixa tudo que a plataforma guarda sobre ela")
    void baixaOsPropriosDados() throws Exception {
        mockMvc.perform(get("/api/cadastro/privacidade/meus-dados")
                        .header("Authorization", "Bearer " + tokenDoEmpreendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conta.email").value("lourdes@exemplo.com"))
                .andExpect(jsonPath("$.negocio.documento").value("529.982.247-25"))
                .andExpect(jsonPath("$.historico").isArray());
    }

    @Test
    @Order(14)
    @DisplayName("14. o pedido de exclusão apaga os dados nos quatro módulos")
    void exclusaoApagaTudo() throws Exception {
        mockMvc.perform(delete("/api/cadastro/privacidade/minha-conta")
                        .header("Authorization", "Bearer " + tokenDoEmpreendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolo").isNotEmpty());

        await().atMost(ESPERA).untilAsserted(() -> {
            // Sumiu da vitrine.
            mockMvc.perform(get("/api/busca/produtos"))
                    .andExpect(jsonPath("$.total").value(0));
            mockMvc.perform(get("/api/busca/lojas/{apelido}", "doces-da-lourdes"))
                    .andExpect(status().isNotFound());

            // A conta foi anonimizada, e não apagada, para a auditoria sobreviver.
            // A busca usa Optional em vez de orElseThrow de propósito: o
            // awaitility só repete quando a falha é AssertionError, e uma
            // exceção comum abortaria a espera na primeira tentativa.
            var conta = usuarios.findAll().stream()
                    .filter(usuario -> usuario.nome().equals("Conta removida"))
                    .findFirst();

            assertThat(conta).isPresent();
            assertThat(conta.get().email()).doesNotContain("lourdes@exemplo.com");
            assertThat(conta.get().ativo()).isFalse();
        });
    }

    @Test
    @Order(15)
    @DisplayName("15. o sistema continua de pé depois de tudo isso")
    void continuaDePe() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/busca/produtos']").exists())
                .andExpect(jsonPath("$.paths['/api/catalogo/meus-produtos']").exists())
                .andExpect(jsonPath("$.paths['/api/cadastro/empreendedores']").exists());

        assertThat(outbox.findAll())
                .as("nenhuma mensagem pode ter ficado travada no caminho")
                .allMatch(mensagem -> mensagem.foiPublicada());
    }
}
