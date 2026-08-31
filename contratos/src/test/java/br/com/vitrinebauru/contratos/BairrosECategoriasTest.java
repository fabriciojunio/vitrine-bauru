package br.com.vitrinebauru.contratos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Bairros de Bauru e categorias")
class BairrosECategoriasTest {

    @Nested
    @DisplayName("Bairros")
    class Bairros {

        static Stream<String> todosOsBairros() {
            return BairrosDeBauru.todos().stream();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("todosOsBairros")
        @DisplayName("todo bairro da lista é reconhecido por ele mesmo")
        void reconheceCadaBairro(String bairro) {
            assertThat(BairrosDeBauru.existe(bairro)).isTrue();
            assertThat(BairrosDeBauru.normalizado(bairro)).contains(bairro);
        }

        @ParameterizedTest(name = "\"{0}\" vira \"{1}\"")
        @CsvSource({
                "'vila cardia', 'Vila Cardia'",
                "'VILA FALCAO', 'Vila Falcão'",
                "'vila falcao', 'Vila Falcão'",
                "'jardim america', 'Jardim América'",
                "'nucleo habitacional presidente geisel', 'Núcleo Habitacional Presidente Geisel'",
                "'tibirica', 'Tibiriçá'",
                "'parque residencial das camelias', 'Parque Residencial das Camélias'",
                "'  centro  ', 'Centro'",
                "'ALTOS DA CIDADE', 'Altos da Cidade'"
        })
        @DisplayName("aceita sem acento e em qualquer caixa, e devolve o nome oficial")
        void aceitaSemAcento(String digitado, String oficial) {
            assertThat(BairrosDeBauru.normalizado(digitado)).contains(oficial);
        }

        @ParameterizedTest(name = "recusa \"{0}\"")
        @ValueSource(strings = {
                "Copacabana", "Vila Madalena", "Centro de Marília", "Bairro Inventado", "xyz"
        })
        @DisplayName("recusa bairro que não é de Bauru")
        void recusaDeFora(String bairro) {
            assertThat(BairrosDeBauru.existe(bairro)).isFalse();
        }

        @Test
        @DisplayName("recusa vazio e nulo")
        void recusaVazio() {
            assertThat(BairrosDeBauru.existe(null)).isFalse();
            assertThat(BairrosDeBauru.existe("")).isFalse();
            assertThat(BairrosDeBauru.existe("   ")).isFalse();
        }

        @Test
        @DisplayName("a lista cobre as regiões da cidade e não tem repetido")
        void listaConsistente() {
            var bairros = BairrosDeBauru.todos();

            assertThat(bairros).hasSizeGreaterThan(30);
            assertThat(bairros).doesNotHaveDuplicates();
            assertThat(bairros).contains("Centro", "Vila Cardia", "Núcleo Habitacional Mary Dota");
        }
    }

    @Nested
    @DisplayName("Categorias")
    class Categorias {

        static Stream<String> todasAsCategorias() {
            return CategoriasDoComercio.todas().stream();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("todasAsCategorias")
        @DisplayName("toda categoria da lista é reconhecida")
        void reconheceCadaCategoria(String categoria) {
            assertThat(CategoriasDoComercio.existe(categoria)).isTrue();
        }

        @ParameterizedTest(name = "\"{0}\" vira \"{1}\"")
        @CsvSource({
                "'alimentacao', 'Alimentação'",
                "'ALIMENTACAO', 'Alimentação'",
                "'beleza e bem-estar', 'Beleza e bem-estar'",
                "'assistencia tecnica', 'Assistência técnica'",
                "'educacao e aulas', 'Educação e aulas'",
                "'saude', 'Saúde'",
                "'moda e acessorios', 'Moda e acessórios'"
        })
        @DisplayName("aceita sem acento e devolve o nome oficial")
        void aceitaSemAcento(String digitada, String oficial) {
            assertThat(CategoriasDoComercio.normalizada(digitada)).contains(oficial);
        }

        @ParameterizedTest(name = "recusa \"{0}\"")
        @ValueSource(strings = {"Mineração", "Tecnologia da Informação", "", "   "})
        @DisplayName("recusa categoria fora da lista")
        void recusaForaDaLista(String categoria) {
            assertThat(CategoriasDoComercio.existe(categoria)).isFalse();
        }

        @Test
        @DisplayName("a lista é curta o bastante para caber num seletor sem assustar")
        void listaCurta() {
            assertThat(CategoriasDoComercio.todas())
                    .hasSizeLessThanOrEqualTo(15)
                    .doesNotHaveDuplicates();
        }
    }
}
