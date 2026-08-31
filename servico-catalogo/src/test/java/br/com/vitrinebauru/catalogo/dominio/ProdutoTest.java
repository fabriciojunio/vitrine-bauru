package br.com.vitrinebauru.catalogo.dominio;

import br.com.vitrinebauru.contratos.tipos.Dinheiro;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Produto")
class ProdutoTest {

    private static final Instant AGORA = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID LOJA = UUID.randomUUID();
    private static final UUID CATEGORIA = UUID.randomUUID();

    private Produto bolo() {
        return Produto.novo(LOJA, "Bolo de pote", "Massa de chocolate com brigadeiro",
                Dinheiro.deCentavos(1200), CATEGORIA, AGORA);
    }

    @Nested
    @DisplayName("ao publicar")
    class AoPublicar {

        @Test
        @DisplayName("nasce disponível e visível na vitrine")
        void nasceDisponivel() {
            var produto = bolo();

            assertThat(produto.disponivel()).isTrue();
            assertThat(produto.apareceNaVitrine()).isTrue();
            assertThat(produto.foiRetirado()).isFalse();
        }

        @Test
        @DisplayName("aceita produto sem preço, que na vitrine vira sob consulta")
        void aceitaSemPreco() {
            var produto = Produto.novo(LOJA, "Móvel sob medida", "Preço conforme o projeto",
                    null, CATEGORIA, AGORA);

            assertThat(produto.temPreco()).isFalse();
            assertThat(produto.precoEmCentavos()).isNull();
            assertThat(produto.preco()).isNull();
        }

        @Test
        @DisplayName("aceita preço zero, que é diferente de sem preço")
        void aceitaPrecoZero() {
            var produto = Produto.novo(LOJA, "Leva e traz no bairro", "Sem custo na região",
                    Dinheiro.ZERO, CATEGORIA, AGORA);

            assertThat(produto.temPreco()).isTrue();
            assertThat(produto.precoEmCentavos()).isZero();
            assertThat(produto.preco().formatado()).isEqualTo("R$ 0,00");
        }

        @ParameterizedTest(name = "recusa nome \"{0}\"")
        @ValueSource(strings = {"", "   "})
        @DisplayName("exige nome")
        void exigeNome(String nome) {
            assertThatThrownBy(() -> Produto.novo(LOJA, nome, null, null, CATEGORIA, AGORA))
                    .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class)
                    .hasMessageContaining("nome do produto");
        }

        @Test
        @DisplayName("recusa nome nulo")
        void recusaNomeNulo() {
            assertThatThrownBy(() -> Produto.novo(LOJA, null, null, null, CATEGORIA, AGORA))
                    .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class);
        }

        @Test
        @DisplayName("recusa nome comprido demais")
        void recusaNomeComprido() {
            assertThatThrownBy(() -> Produto.novo(LOJA, "a".repeat(121), null, null, CATEGORIA, AGORA))
                    .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class)
                    .hasMessageContaining("120");
        }

        @Test
        @DisplayName("tira espaço sobrando do nome")
        void tiraEspacoDoNome() {
            var produto = Produto.novo(LOJA, "  Pastel de feira  ", null, null, CATEGORIA, AGORA);

            assertThat(produto.nome()).isEqualTo("Pastel de feira");
        }
    }

    @Nested
    @DisplayName("disponibilidade")
    class Disponibilidade {

        @Test
        @DisplayName("marcar como esgotado tira da vitrine sem apagar o produto")
        void esgotadoSaiDaVitrine() {
            var produto = bolo();

            produto.marcarDisponivel(false, AGORA.plusSeconds(60));

            assertThat(produto.disponivel()).isFalse();
            assertThat(produto.apareceNaVitrine()).isFalse();
            assertThat(produto.foiRetirado()).isFalse();
            assertThat(produto.nome()).isEqualTo("Bolo de pote");
        }

        @Test
        @DisplayName("volta a aparecer quando o estoque volta")
        void voltaAAparecer() {
            var produto = bolo();
            produto.marcarDisponivel(false, AGORA);

            produto.marcarDisponivel(true, AGORA.plusSeconds(3600));

            assertThat(produto.apareceNaVitrine()).isTrue();
        }
    }

    @Nested
    @DisplayName("retirada do catálogo")
    class Retirada {

        @Test
        @DisplayName("retirar tira da vitrine e marca a data")
        void retirar() {
            var produto = bolo();

            produto.retirar(AGORA.plusSeconds(120));

            assertThat(produto.foiRetirado()).isTrue();
            assertThat(produto.retiradoEm()).isEqualTo(AGORA.plusSeconds(120));
            assertThat(produto.apareceNaVitrine()).isFalse();
            assertThat(produto.disponivel()).isFalse();
        }

        @Test
        @DisplayName("retirar duas vezes é conflito")
        void retirarDuasVezes() {
            var produto = bolo();
            produto.retirar(AGORA);

            assertThatThrownBy(() -> produto.retirar(AGORA.plusSeconds(60)))
                    .isInstanceOf(ErrosDeNegocio.Conflito.class);
        }

        @Test
        @DisplayName("produto retirado não aceita mais alteração")
        void retiradoNaoAltera() {
            var produto = bolo();
            produto.retirar(AGORA);

            assertThatThrownBy(() -> produto.alterar("Outro nome", null, null, CATEGORIA, AGORA))
                    .isInstanceOf(ErrosDeNegocio.Conflito.class);
            assertThatThrownBy(() -> produto.marcarDisponivel(true, AGORA))
                    .isInstanceOf(ErrosDeNegocio.Conflito.class);
            assertThatThrownBy(() -> produto.trocarImagem(UUID.randomUUID(), AGORA))
                    .isInstanceOf(ErrosDeNegocio.Conflito.class);
        }
    }

    @Nested
    @DisplayName("dono")
    class Dono {

        @Test
        @DisplayName("reconhece o dono e recusa qualquer outro")
        void reconheceODono() {
            var produto = bolo();

            assertThat(produto.pertenceA(LOJA)).isTrue();
            assertThat(produto.pertenceA(UUID.randomUUID())).isFalse();
        }
    }

    @Nested
    @DisplayName("alteração")
    class Alteracao {

        @Test
        @DisplayName("troca nome, descrição, preço e categoria de uma vez")
        void alteraTudo() {
            var produto = bolo();
            UUID outraCategoria = UUID.randomUUID();

            produto.alterar("Bolo de pote grande", "Agora com 350ml",
                    Dinheiro.deCentavos(1800), outraCategoria, AGORA.plusSeconds(60));

            assertThat(produto.nome()).isEqualTo("Bolo de pote grande");
            assertThat(produto.descricao()).isEqualTo("Agora com 350ml");
            assertThat(produto.precoEmCentavos()).isEqualTo(1800);
            assertThat(produto.categoriaId()).isEqualTo(outraCategoria);
            assertThat(produto.atualizadoEm()).isEqualTo(AGORA.plusSeconds(60));
        }

        @Test
        @DisplayName("tirar o preço transforma em sob consulta")
        void tiraOPreco() {
            var produto = bolo();

            produto.alterar("Bolo de pote", null, null, CATEGORIA, AGORA);

            assertThat(produto.temPreco()).isFalse();
        }

        @ParameterizedTest(name = "{0} centavos formatam como {1}")
        @CsvSource({
                "1200, 'R$ 12,00'",
                "1250, 'R$ 12,50'",
                "9000, 'R$ 90,00'",
                "28000, 'R$ 280,00'",
                "123456, 'R$ 1.234,56'"
        })
        @DisplayName("o preço formatado é o que a vitrine mostra")
        void precoFormatado(long centavos, String esperado) {
            var produto = Produto.novo(LOJA, "Produto", null,
                    Dinheiro.deCentavos(centavos), CATEGORIA, AGORA);

            assertThat(produto.preco().formatado()).isEqualTo(esperado);
        }
    }

    @Nested
    @DisplayName("imagem")
    class Imagem {

        @Test
        @DisplayName("nasce sem foto e aceita uma depois")
        void trocaImagem() {
            var produto = bolo();
            UUID imagem = UUID.randomUUID();

            assertThat(produto.imagemId()).isNull();

            produto.trocarImagem(imagem, AGORA.plusSeconds(30));

            assertThat(produto.imagemId()).isEqualTo(imagem);
        }
    }
}
