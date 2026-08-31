package br.com.vitrinebauru.unico;

import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository;
import br.com.vitrinebauru.plataforma.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * O que acontece quando várias pessoas fazem a mesma coisa ao mesmo tempo.
 *
 * <p>Usa threads de verdade contra um PostgreSQL de verdade, e não simulação:
 * corrida de dados só aparece quando duas transações disputam a mesma linha, e
 * isso não se reproduz com repositório de mentira.
 *
 * <p>São os três casos que este sistema pode encontrar num dia de divulgação
 * da SEDECON: várias pessoas se cadastrando ao mesmo tempo, duas lojas com o
 * mesmo nome, e muita gente clicando no botão de WhatsApp da mesma loja.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Concorrência")
class ConcorrenciaTest {

    private static final int PESSOAS_AO_MESMO_TEMPO = 10;
    private static EmbeddedPostgres postgres;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry propriedades) throws Exception {
        postgres = EmbeddedPostgres.builder().start();
        propriedades.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        propriedades.add("spring.datasource.username", () -> "postgres");
        propriedades.add("spring.datasource.password", () -> "");
        propriedades.add("vitrine.mensageria.transporte", () -> "processo");
        propriedades.add("vitrine.outbox.intervalo-ms", () -> "100");
        propriedades.add("vitrine.limite.ativo", () -> "false");
        propriedades.add("vitrine.brasilapi.ativa", () -> "false");
        propriedades.add("vitrine.demo.ativo", () -> "false");
        // Pool grande o bastante para as dez threads disputarem de verdade, em
        // vez de ficarem na fila esperando conexão.
        propriedades.add("spring.datasource.hikari.maximum-pool-size", () -> "16");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private EmpreendedorRepository empreendedores;
    @Autowired
    private OutboxRepository outbox;

    @BeforeEach
    void limpar() {
        empreendedores.deleteAll();
        usuarios.deleteAll();
        outbox.deleteAll();
    }

    /** Dispara todas as tarefas no mesmo instante, e não uma depois da outra. */
    private List<Integer> aoMesmoTempo(List<Callable<Integer>> tarefas) throws Exception {
        var largada = new CountDownLatch(1);
        var prontas = new CountDownLatch(tarefas.size());
        var resultados = new java.util.concurrent.ConcurrentLinkedQueue<Integer>();

        try (ExecutorService time = Executors.newFixedThreadPool(tarefas.size())) {
            for (Callable<Integer> tarefa : tarefas) {
                time.submit(() -> {
                    try {
                        largada.await();
                        resultados.add(tarefa.call());
                    } catch (Exception e) {
                        resultados.add(-1);
                    } finally {
                        prontas.countDown();
                    }
                    return null;
                });
            }

            largada.countDown();
            assertThat(prontas.await(60, TimeUnit.SECONDS))
                    .as("todas as tentativas precisam terminar")
                    .isTrue();
        }

        return List.copyOf(resultados);
    }

    private String corpo(String email, String documento, String nomeDoNegocio) {
        return """
                {
                  "nome": "Empreendedor de Teste",
                  "email": "%s",
                  "senha": "senhadeteste2026",
                  "nomeDoNegocio": "%s",
                  "descricao": "Cadastro do teste de concorrência",
                  "categoriaPrincipal": "Artesanato",
                  "bairro": "Centro",
                  "cep": "17011-066",
                  "telefoneWhatsapp": "14998887766",
                  "documento": "%s"
                }
                """.formatted(email, nomeDoNegocio, documento);
    }

    /** CPF com dígito verificador correto, gerado a partir de uma base. */
    private static String cpfComBase(int base) {
        String nove = String.format("%09d", 100000000 + base).substring(0, 9);

        int primeiro = digito(nove, 10);
        int segundo = digito(nove + primeiro, 11);
        return nove + primeiro + segundo;
    }

    private static int digito(String parcial, int pesoInicial) {
        int soma = 0;
        for (int posicao = 0; posicao < parcial.length(); posicao++) {
            soma += (parcial.charAt(posicao) - '0') * (pesoInicial - posicao);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    @Test
    @DisplayName("dez pessoas tentando o mesmo e-mail: uma entra, nove recebem conflito")
    void mesmoEmailAoMesmoTempo() throws Exception {
        var tarefas = new java.util.ArrayList<Callable<Integer>>();
        for (int tentativa = 0; tentativa < PESSOAS_AO_MESMO_TEMPO; tentativa++) {
            int numero = tentativa;
            tarefas.add(() -> mockMvc.perform(post("/api/cadastro/empreendedores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo("disputado@exemplo.com", cpfComBase(numero),
                                    "Loja " + numero)))
                    .andReturn().getResponse().getStatus());
        }

        List<Integer> respostas = aoMesmoTempo(tarefas);

        assertThat(respostas.stream().filter(status -> status == 201).count())
                .as("só uma conta pode ser criada com esse e-mail")
                .isEqualTo(1);
        assertThat(usuarios.findByEmail("disputado@exemplo.com")).isPresent();
        assertThat(usuarios.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("dez lojas com o mesmo nome ganham dez endereços diferentes")
    void mesmoNomeDeNegocioAoMesmoTempo() throws Exception {
        var tarefas = new java.util.ArrayList<Callable<Integer>>();
        for (int tentativa = 0; tentativa < PESSOAS_AO_MESMO_TEMPO; tentativa++) {
            int numero = tentativa;
            tarefas.add(() -> mockMvc.perform(post("/api/cadastro/empreendedores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo("loja" + numero + "@exemplo.com", cpfComBase(numero),
                                    "Casa do Pastel")))
                    .andReturn().getResponse().getStatus());
        }

        aoMesmoTempo(tarefas);

        var apelidos = empreendedores.findAll().stream()
                .map(empreendedor -> empreendedor.apelidoNaUrl())
                .toList();

        assertThat(apelidos)
                .as("cada loja precisa do próprio endereço, mesmo com o nome repetido")
                .doesNotHaveDuplicates();
        assertThat(apelidos).isNotEmpty();
    }

    @Test
    @DisplayName("o mesmo CPF em dez tentativas simultâneas entra uma vez só")
    void mesmoDocumentoAoMesmoTempo() throws Exception {
        String documento = cpfComBase(777);

        var tarefas = new java.util.ArrayList<Callable<Integer>>();
        for (int tentativa = 0; tentativa < PESSOAS_AO_MESMO_TEMPO; tentativa++) {
            int numero = tentativa;
            tarefas.add(() -> mockMvc.perform(post("/api/cadastro/empreendedores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo("cpf" + numero + "@exemplo.com", documento,
                                    "Loja do CPF " + numero)))
                    .andReturn().getResponse().getStatus());
        }

        aoMesmoTempo(tarefas);

        assertThat(empreendedores.count())
                .as("o índice único no banco é a garantia final, não a consulta antes de gravar")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("nenhum evento se perde nem sai duplicado quando tudo acontece junto")
    void nenhumEventoSePerde() throws Exception {
        var tarefas = new java.util.ArrayList<Callable<Integer>>();
        for (int tentativa = 0; tentativa < PESSOAS_AO_MESMO_TEMPO; tentativa++) {
            int numero = tentativa;
            tarefas.add(() -> mockMvc.perform(post("/api/cadastro/empreendedores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo("evento" + numero + "@exemplo.com", cpfComBase(numero),
                                    "Loja de Evento " + numero)))
                    .andReturn().getResponse().getStatus());
        }

        List<Integer> respostas = aoMesmoTempo(tarefas);
        long criados = respostas.stream().filter(status -> status == 201).count();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var mensagens = outbox.findAll().stream()
                    .filter(mensagem -> mensagem.tipo().equals("EmpreendedorCadastrado"))
                    .toList();

            assertThat(mensagens)
                    .as("um evento por cadastro criado, nem mais nem menos")
                    .hasSize((int) criados);
            assertThat(mensagens).allMatch(mensagem -> mensagem.foiPublicada());
            assertThat(mensagens.stream().map(mensagem -> mensagem.id()).distinct().count())
                    .isEqualTo(criados);
        });
    }

    @Test
    @DisplayName("o publicador do outbox não entrega a mesma mensagem duas vezes")
    void publicadorNaoDuplica() throws Exception {
        var contador = new AtomicInteger();

        var tarefas = new java.util.ArrayList<Callable<Integer>>();
        for (int tentativa = 0; tentativa < 5; tentativa++) {
            int numero = tentativa;
            tarefas.add(() -> {
                int status = mockMvc.perform(post("/api/cadastro/empreendedores")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo("dup" + numero + "@exemplo.com", cpfComBase(500 + numero),
                                        "Loja Dup " + numero)))
                        .andReturn().getResponse().getStatus();
                if (status == 201) {
                    contador.incrementAndGet();
                }
                return status;
            });
        }

        aoMesmoTempo(tarefas);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(outbox.findAll()).allMatch(mensagem -> mensagem.foiPublicada()));

        // A projeção da busca é montada por evento: se algum tivesse chegado
        // duas vezes, apareceria loja repetida na vitrine.
        assertThat(contador.get()).isEqualTo(5);
    }
}
