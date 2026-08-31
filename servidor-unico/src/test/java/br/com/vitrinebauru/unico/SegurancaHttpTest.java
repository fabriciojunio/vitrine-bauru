package br.com.vitrinebauru.unico;

import br.com.vitrinebauru.plataforma.seguranca.EmissorDeToken;
import br.com.vitrinebauru.plataforma.seguranca.Papel;
import br.com.vitrinebauru.plataforma.seguranca.UsuarioAutenticado;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A superfície exposta na internet, conferida de fora.
 *
 * <p>Não é teste de biblioteca de segurança: é teste da configuração deste
 * sistema. Cabeçalho que ninguém verifica some numa refatoração, e a falta só
 * aparece quando alguém procura.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Segurança da API")
class SegurancaHttpTest {

    private static EmbeddedPostgres postgres;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry propriedades) throws Exception {
        postgres = EmbeddedPostgres.builder().start();
        propriedades.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        propriedades.add("spring.datasource.username", () -> "postgres");
        propriedades.add("spring.datasource.password", () -> "");
        propriedades.add("vitrine.mensageria.transporte", () -> "processo");
        propriedades.add("vitrine.demo.ativo", () -> "false");
        propriedades.add("vitrine.brasilapi.ativa", () -> "false");
        propriedades.add("vitrine.seguranca.origens-permitidas", () -> "https://vitrinebauru.vercel.app");
        // O limite fica ligado neste teste: é justamente ele que se quer ver.
        propriedades.add("vitrine.limite.ativo", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EmissorDeToken emissor;

    private String tokenDe(Papel papel) {
        return emissor.emitir(new UsuarioAutenticado(
                UUID.randomUUID(), "teste@exemplo.com", papel,
                papel == Papel.EMPREENDEDOR ? UUID.randomUUID() : null));
    }

    @Nested
    @DisplayName("cabeçalhos")
    class Cabecalhos {

        @Test
        @DisplayName("manda política de conteúdo, para o navegador não executar o que não veio daqui")
        void politicaDeConteudo() throws Exception {
            mockMvc.perform(get("/api/busca/produtos"))
                    .andExpect(header().string("Content-Security-Policy",
                            org.hamcrest.Matchers.containsString("default-src 'none'")));
        }

        @Test
        @DisplayName("proíbe a API de ser aberta dentro de um quadro de outro site")
        void proibeQuadro() throws Exception {
            mockMvc.perform(get("/api/busca/produtos"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Content-Security-Policy",
                            org.hamcrest.Matchers.containsString("frame-ancestors 'none'")));
        }

        @Test
        @DisplayName("não deixa o endereço da nossa página vazar para o site de destino")
        void naoVazaReferencia() throws Exception {
            mockMvc.perform(get("/api/busca/produtos"))
                    .andExpect(header().string("Referrer-Policy", "no-referrer"));
        }

        @Test
        @DisplayName("toda resposta carrega a correlação, inclusive as de erro")
        void correlacaoSempre() throws Exception {
            mockMvc.perform(get("/api/busca/produtos"))
                    .andExpect(header().exists("X-Correlacao"));

            mockMvc.perform(get("/api/cadastro/minha-loja"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().exists("X-Correlacao"));
        }

        @Test
        @DisplayName("aceita a correlação que o cliente mandou, para rastrear ponta a ponta")
        void aproveitaACorrelacaoDoCliente() throws Exception {
            String correlacao = UUID.randomUUID().toString();

            mockMvc.perform(get("/api/busca/produtos").header("X-Correlacao", correlacao))
                    .andExpect(header().string("X-Correlacao", correlacao));
        }
    }

    @Nested
    @DisplayName("quem pode o quê")
    class QuemPodeOQue {

        @ParameterizedTest(name = "{0} exige login")
        @ValueSource(strings = {
                "/api/cadastro/minha-loja",
                "/api/cadastro/minha-loja/indicadores",
                "/api/catalogo/meus-produtos",
                "/api/cadastro/moderacao/fila",
                "/api/cadastro/moderacao/indicadores",
                "/api/notificacoes",
                "/api/cadastro/privacidade/meus-dados"
        })
        @DisplayName("área logada não abre sem token")
        void areaLogadaExigeToken(String caminho) throws Exception {
            mockMvc.perform(get(caminho))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Faça login para continuar."));
        }

        @ParameterizedTest(name = "{0} é público")
        @ValueSource(strings = {
                "/api/busca/produtos",
                "/api/busca/lojas",
                "/api/busca/resumo",
                "/api/cadastro/bairros",
                "/api/cadastro/categorias",
                "/api/catalogo/categorias",
                "/actuator/health"
        })
        @DisplayName("a vitrine e o que ela precisa continuam abertos")
        void vitrineEhPublica(String caminho) throws Exception {
            mockMvc.perform(get(caminho)).andExpect(status().isOk());
        }

        @ParameterizedTest(name = "empreendedor não entra em {0}")
        @ValueSource(strings = {
                "/api/cadastro/moderacao/fila",
                "/api/cadastro/moderacao/indicadores",
                "/api/cadastro/moderacao/auditoria",
                "/api/notificacoes",
                "/actuator/prometheus"
        })
        @DisplayName("empreendedor não alcança a área da SEDECON")
        void empreendedorNaoAlcancaModeracao(String caminho) throws Exception {
            mockMvc.perform(get(caminho).header("Authorization", "Bearer " + tokenDe(Papel.EMPREENDEDOR)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("administrador alcança a moderação")
        void administradorAlcancaModeracao() throws Exception {
            mockMvc.perform(get("/api/cadastro/moderacao/fila")
                            .header("Authorization", "Bearer " + tokenDe(Papel.ADMIN_SEDECON)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("token adulterado é recusado")
        void tokenAdulterado() throws Exception {
            String token = tokenDe(Papel.ADMIN_SEDECON);
            String adulterado = token.substring(0, token.length() - 3) + "aaa";

            mockMvc.perform(get("/api/cadastro/moderacao/fila")
                            .header("Authorization", "Bearer " + adulterado))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("cabeçalho sem o prefixo Bearer é ignorado")
        void cabecalhoSemPrefixo() throws Exception {
            mockMvc.perform(get("/api/cadastro/minha-loja")
                            .header("Authorization", tokenDe(Papel.EMPREENDEDOR)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("origem cruzada")
    class OrigemCruzada {

        @Test
        @DisplayName("aceita o domínio do frontend configurado")
        void aceitaODominioConfigurado() throws Exception {
            mockMvc.perform(options("/api/busca/produtos")
                            .header("Origin", "https://vitrinebauru.vercel.app")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin",
                            "https://vitrinebauru.vercel.app"));
        }

        @Test
        @DisplayName("recusa origem desconhecida, em vez de liberar para qualquer site")
        void recusaOrigemDesconhecida() throws Exception {
            mockMvc.perform(options("/api/busca/produtos")
                            .header("Origin", "https://site-que-copiou.invalido")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("limite de requisições")
    class LimiteDeRequisicoes {

        @Test
        @DisplayName("segura a força bruta no login depois de cinco tentativas por minuto")
        void seguraForcaBrutaNoLogin() throws Exception {
            String corpo = "{\"email\":\"alguem@exemplo.com\",\"senha\":\"tentativa123\"}";

            for (int tentativa = 0; tentativa < 5; tentativa++) {
                mockMvc.perform(post("/api/cadastro/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo)
                        .with(requisicao -> {
                            requisicao.setRemoteAddr("203.0.113.10");
                            return requisicao;
                        }));
            }

            mockMvc.perform(post("/api/cadastro/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo)
                            .with(requisicao -> {
                                requisicao.setRemoteAddr("203.0.113.10");
                                return requisicao;
                            }))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().exists("Retry-After"));
        }

        @Test
        @DisplayName("a vitrine pública não é limitada junto com o login")
        void vitrineNaoEhLimitadaJunto() throws Exception {
            for (int consulta = 0; consulta < 20; consulta++) {
                mockMvc.perform(get("/api/busca/produtos"))
                        .andExpect(status().isOk());
            }
        }
    }

    @Nested
    @DisplayName("entrada maliciosa")
    class EntradaMaliciosa {

        @ParameterizedTest(name = "busca com {0} não quebra nem vaza")
        @ValueSource(strings = {
                "' OR '1'='1",
                "'; drop table busca.produto; --",
                "1 UNION SELECT senha_hash FROM cadastro.usuario",
                "<script>alert(1)</script>",
                "../../etc/passwd",
                "%00",
                "{{7*7}}"
        })
        @DisplayName("termo de busca hostil devolve resposta normal")
        void termoHostil(String termo) throws Exception {
            mockMvc.perform(get("/api/busca/produtos").param("termo", termo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conteudo").isArray());
        }

        @Test
        @DisplayName("a tabela continua lá depois da tentativa de apagar")
        void tabelaContinuaLa() throws Exception {
            mockMvc.perform(get("/api/busca/produtos")
                    .param("termo", "'; drop table busca.produto; --"));

            mockMvc.perform(get("/api/busca/produtos"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("erro interno não devolve pilha nem nome de tabela")
        void erroNaoVazaDetalhe() throws Exception {
            mockMvc.perform(get("/api/busca/lojas/{apelido}", "nao-existe"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Essa loja não está disponível."))
                    .andExpect(jsonPath("$.trace").doesNotExist())
                    .andExpect(jsonPath("$.exception").doesNotExist());
        }

        @Test
        @DisplayName("página gigante é cortada no teto, e não vira negação de serviço")
        void paginaGiganteEhCortada() throws Exception {
            mockMvc.perform(get("/api/busca/produtos").param("tamanho", "100000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tamanho").value(48));
        }
    }
}
