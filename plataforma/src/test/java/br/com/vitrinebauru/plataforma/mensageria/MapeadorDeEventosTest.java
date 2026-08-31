package br.com.vitrinebauru.plataforma.mensageria;

import br.com.vitrinebauru.contratos.CadastroAprovado;
import br.com.vitrinebauru.contratos.CadastroRejeitado;
import br.com.vitrinebauru.contratos.CanalDeContato;
import br.com.vitrinebauru.contratos.ContatoIniciado;
import br.com.vitrinebauru.contratos.EmpreendedorCadastrado;
import br.com.vitrinebauru.contratos.EmpreendedorReativado;
import br.com.vitrinebauru.contratos.EmpreendedorSuspenso;
import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.contratos.ExclusaoConcluida;
import br.com.vitrinebauru.contratos.ExclusaoSolicitada;
import br.com.vitrinebauru.contratos.ExpurgoConcluido;
import br.com.vitrinebauru.contratos.OrigemDoContato;
import br.com.vitrinebauru.contratos.Participante;
import br.com.vitrinebauru.contratos.PerfilAtualizado;
import br.com.vitrinebauru.contratos.ProdutoAtualizado;
import br.com.vitrinebauru.contratos.ProdutoPublicado;
import br.com.vitrinebauru.contratos.ProdutoRetirado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato de serializacao dos eventos.
 *
 * <p>E o teste que segura a compatibilidade entre servicos: quem renomear um
 * campo de evento quebra aqui, no build, e nao em producao, com o consumidor
 * do outro lado recebendo nulo em silencio.
 */
@DisplayName("Mapeador de eventos")
class MapeadorDeEventosTest {

    private static final Instant QUANDO = Instant.parse("2026-09-22T15:30:00Z");
    private static final UUID EMPREENDEDOR = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final MapeadorDeEventos mapeador = new MapeadorDeEventos();

    static Stream<Evento> todosOsEventos() {
        UUID id = UUID.randomUUID();
        UUID correlacao = UUID.randomUUID();
        UUID produto = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID categoria = UUID.randomUUID();

        return Stream.of(
                new EmpreendedorCadastrado(id, correlacao, QUANDO, EMPREENDEDOR, usuario,
                        "Doces da Lourdes", "doces-da-lourdes", "Bolo de pote e salgado de festa",
                        "Alimentação", "Vila Cardia", "14997123456", "52998224725",
                        "lourdes@exemplo.com", "Maria de Lourdes"),
                new PerfilAtualizado(id, correlacao, QUANDO, EMPREENDEDOR, "Doces da Lourdes",
                        "doces-da-lourdes", "Agora com salgado assado", "Alimentação",
                        "Vila Cardia", "14997123456", "https://exemplo.invalido/capa.webp"),
                new CadastroAprovado(id, correlacao, QUANDO, EMPREENDEDOR, usuario,
                        "Doces da Lourdes", "lourdes@exemplo.com", "Maria de Lourdes"),
                new CadastroRejeitado(id, correlacao, QUANDO, EMPREENDEDOR, usuario,
                        "Documento não confere com o nome informado", "Doces da Lourdes",
                        "lourdes@exemplo.com", "Maria de Lourdes"),
                new EmpreendedorSuspenso(id, correlacao, QUANDO, EMPREENDEDOR, usuario,
                        "Denúncia de propaganda enganosa", "Doces da Lourdes",
                        "lourdes@exemplo.com", "Maria de Lourdes"),
                new EmpreendedorReativado(id, correlacao, QUANDO, EMPREENDEDOR, usuario,
                        "Doces da Lourdes", "lourdes@exemplo.com", "Maria de Lourdes"),
                new ProdutoPublicado(id, correlacao, QUANDO, produto, EMPREENDEDOR,
                        "Bolo de pote", "Massa de chocolate com brigadeiro", 1250L,
                        categoria, "Alimentação", "https://exemplo.invalido/bolo.webp", true),
                new ProdutoAtualizado(id, correlacao, QUANDO, produto, EMPREENDEDOR,
                        "Bolo de pote", "Agora também de ninho", null,
                        categoria, "Alimentação", null, false),
                new ProdutoRetirado(id, correlacao, QUANDO, produto, EMPREENDEDOR),
                new ContatoIniciado(id, correlacao, QUANDO, EMPREENDEDOR, produto,
                        CanalDeContato.WHATSAPP, OrigemDoContato.PAGINA_DO_PRODUTO),
                new ExclusaoSolicitada(id, correlacao, QUANDO, EMPREENDEDOR, usuario,
                        QUANDO.plusSeconds(1_296_000)),
                new ExpurgoConcluido(id, correlacao, QUANDO, EMPREENDEDOR, Participante.CATALOGO, 7),
                new ExclusaoConcluida(id, correlacao, QUANDO, EMPREENDEDOR, usuario));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todosOsEventos")
    @DisplayName("todo evento volta igual depois de virar JSON")
    void idaEVolta(Evento original) {
        String json = mapeador.paraJson(original);

        Evento lido = mapeador.deJson(json);

        assertThat(lido).isEqualTo(original);
        assertThat(lido.getClass()).isEqualTo(original.getClass());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todosOsEventos")
    @DisplayName("o JSON carrega o campo tipo, que e como o outro lado sabe o que chegou")
    void carregaOTipo(Evento original) {
        assertThat(mapeador.paraJson(original))
                .contains("\"tipo\":\"" + original.tipoDoEvento() + "\"");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todosOsEventos")
    @DisplayName("a data sai como texto ISO, e nao como numero de milissegundos")
    void dataEmFormatoLegivel(Evento original) {
        assertThat(mapeador.paraJson(original)).contains("2026-09-22T15:30:00Z");
    }

    @Test
    @DisplayName("aceita campo desconhecido, para o servico antigo ler evento da versao nova")
    void aceitaCampoNovo() {
        String json = """
                {"tipo":"ProdutoRetirado","id":"%s","correlacao":"%s","ocorridoEm":"2026-09-22T15:30:00Z",
                 "produtoId":"%s","empreendedorId":"%s","campoQueAindaNaoExiste":"valor"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), EMPREENDEDOR);

        Evento lido = mapeador.deJson(json);

        assertThat(lido).isInstanceOf(ProdutoRetirado.class);
    }

    @Test
    @DisplayName("recusa JSON sem tipo, porque nao da para adivinhar o que e")
    void recusaSemTipo() {
        assertThatThrownBy(() -> mapeador.deJson("{\"empreendedorId\":\"" + EMPREENDEDOR + "\"}"))
                .isInstanceOf(MapeadorDeEventos.EventoIlegivel.class);
    }

    @Test
    @DisplayName("recusa texto que nao e JSON")
    void recusaLixo() {
        assertThatThrownBy(() -> mapeador.deJson("nao e json"))
                .isInstanceOf(MapeadorDeEventos.EventoIlegivel.class);
    }

    @Test
    @DisplayName("a chave de particao e sempre o empreendedor, para preservar a ordem")
    void chaveDeParticao() {
        todosOsEventos().forEach(evento ->
                assertThat(evento.chaveDeParticao()).isEqualTo(EMPREENDEDOR));
    }

    @Test
    @DisplayName("preserva acento no meio do texto")
    void preservaAcento() {
        var evento = new CadastroRejeitado(UUID.randomUUID(), UUID.randomUUID(), QUANDO,
                EMPREENDEDOR, UUID.randomUUID(),
                "Endereço não confere com a inscrição", "Açaí do João",
                "joao@exemplo.com", "João");

        var lido = (CadastroRejeitado) mapeador.deJson(mapeador.paraJson(evento));

        assertThat(lido.motivo()).isEqualTo("Endereço não confere com a inscrição");
        assertThat(lido.nomeDoNegocio()).isEqualTo("Açaí do João");
    }
}
