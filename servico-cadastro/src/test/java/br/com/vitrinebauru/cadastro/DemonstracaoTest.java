package br.com.vitrinebauru.cadastro;

import br.com.vitrinebauru.cadastro.aplicacao.EntrarComoDemonstracao;
import br.com.vitrinebauru.cadastro.dominio.Usuario;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.contratos.demonstracao.DadosDaDemonstracao;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A demonstração pública com o modo ligado e os dados semeados de verdade.
 *
 * <p>O que se prova aqui é que um visitante não consegue tirar do ar a loja
 * em que o próximo visitante vai entrar, nem pela exclusão de dados nem pela
 * suspensão, e que as outras lojas continuam servindo para testar as duas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EmbeddedKafka(
        partitions = 1,
        topics = {Topicos.EMPREENDEDORES, Topicos.CATALOGO, Topicos.CONTATOS, Topicos.PRIVACIDADE,
                Topicos.EMPREENDEDORES + ".dlq", Topicos.CATALOGO + ".dlq",
                Topicos.CONTATOS + ".dlq", Topicos.PRIVACIDADE + ".dlq"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
@DisplayName("Demonstração pública")
class DemonstracaoTest {

    private static EmbeddedPostgres postgres;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry propriedades) throws Exception {
        postgres = EmbeddedPostgres.builder().start();
        propriedades.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        propriedades.add("spring.datasource.username", () -> "postgres");
        propriedades.add("spring.datasource.password", () -> "");
        propriedades.add("spring.kafka.bootstrap-servers", () -> "${spring.embedded.kafka.brokers}");
        propriedades.add("vitrine.brasilapi.ativa", () -> "false");
        propriedades.add("vitrine.conferencia.intervalo-ms", () -> "3600000");
        propriedades.add("vitrine.limite.ativo", () -> "false");
        propriedades.add("vitrine.demo.ativo", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private EmpreendedorRepository empreendedores;

    private String entrarComo(String papel) throws Exception {
        var resultado = mockMvc.perform(post("/api/cadastro/auth/demonstracao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papel\":\"" + papel + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("tokenDeAcesso").asText();
    }

    private UUID lojaDe(String email) {
        var usuario = usuarios.findByEmail(Usuario.normalizarEmail(email)).orElseThrow();
        return empreendedores.findByUsuarioId(usuario.id()).orElseThrow().id();
    }

    @Test
    @DisplayName("quem entra pelo botão não consegue pedir a exclusão da loja de entrada")
    void naoExcluiALojaDeEntrada() throws Exception {
        String token = entrarComo(EntrarComoDemonstracao.PAPEL_EMPREENDEDOR);

        mockMvc.perform(delete("/api/cadastro/privacidade/minha-conta")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(empreendedores.findById(lojaDe(EntrarComoDemonstracao.emailDaLojaDeEntrada()))
                .orElseThrow().status().name()).isEqualTo("APROVADO");
        entrarComo(EntrarComoDemonstracao.PAPEL_EMPREENDEDOR);
    }

    @Test
    @DisplayName("a SEDECON da demonstração não suspende a loja de entrada, mas suspende as outras")
    void suspendeSoAsOutras() throws Exception {
        String token = entrarComo(EntrarComoDemonstracao.PAPEL_SEDECON);
        String motivo = "{\"motivo\":\"Teste de moderação na demonstração\"}";

        mockMvc.perform(post("/api/cadastro/moderacao/{id}/suspender",
                                lojaDe(EntrarComoDemonstracao.emailDaLojaDeEntrada()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(motivo))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/cadastro/moderacao/{id}/suspender",
                                lojaDe(DadosDaDemonstracao.lojas().get(1).email()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(motivo))
                .andExpect(status().isNoContent());
    }
}
