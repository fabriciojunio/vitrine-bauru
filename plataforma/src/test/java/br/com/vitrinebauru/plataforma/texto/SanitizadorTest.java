package br.com.vitrinebauru.plataforma.texto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As cargas usadas aqui são as clássicas de XSS, do jeito que apareceriam num
 * campo de descrição de produto aberto ao público.
 */
@DisplayName("Sanitizador de texto livre")
class SanitizadorTest {

    private final Sanitizador sanitizador = new Sanitizador();

    @ParameterizedTest(name = "neutraliza {0}")
    @ValueSource(strings = {
            "<script>alert(1)</script>",
            "<img src=x onerror=alert(1)>",
            "<svg/onload=alert(1)>",
            "<iframe src=\"https://exemplo.invalido\"></iframe>",
            "<a href=\"javascript:alert(1)\">clique</a>",
            "<body onload=alert(1)>",
            "<input onfocus=alert(1) autofocus>",
            "<details open ontoggle=alert(1)>",
            "<style>@import 'https://exemplo.invalido';</style>",
            "<object data=\"data:text/html;base64,PHNjcmlwdD4=\"></object>"
    })
    @DisplayName("nao deixa passar marcacao executavel")
    void neutralizaCargasDeXss(String carga) {
        String limpo = sanitizador.limpar(carga);

        assertThat(limpo).doesNotContain("<");
        assertThat(limpo).doesNotContain(">");
        assertThat(limpo.toLowerCase()).doesNotContain("onerror");
        assertThat(limpo.toLowerCase()).doesNotContain("onload");
        assertThat(limpo.toLowerCase()).doesNotContain("javascript:");
    }

    @ParameterizedTest(name = "\"{0}\" continua \"{1}\"")
    @CsvSource({
            "'Bolo de pote de morango', 'Bolo de pote de morango'",
            "'Doces & Salgados da Lourdes', 'Doces & Salgados da Lourdes'",
            "'Conserto de máquina de lavar', 'Conserto de máquina de lavar'",
            "'Marmita fitness (500g)', 'Marmita fitness (500g)'",
            "'Preço: R$ 12,50 a unidade', 'Preço: R$ 12,50 a unidade'",
            "'Aceito Pix, cartão e dinheiro', 'Aceito Pix, cartão e dinheiro'",
            "'Tamanhos P/M/G', 'Tamanhos P/M/G'",
            "'Peça com 24h de antecedência', 'Peça com 24h de antecedência'"
    })
    @DisplayName("nao estraga texto legitimo de empreendedor")
    void mantemTextoLegitimo(String original, String esperado) {
        assertThat(sanitizador.limpar(original)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("mantem o texto de dentro da marcacao removida")
    void mantemConteudoDeDentro() {
        assertThat(sanitizador.limpar("<b>Bolo</b> caseiro")).isEqualTo("Bolo caseiro");
    }

    @Test
    @DisplayName("tira espaco sobrando nas pontas")
    void tiraEspacoDasPontas() {
        assertThat(sanitizador.limpar("   pastel de feira   ")).isEqualTo("pastel de feira");
    }

    @Test
    @DisplayName("deixa nulo passar como nulo, para o campo opcional continuar opcional")
    void aceitaNulo() {
        assertThat(sanitizador.limpar(null)).isNull();
        assertThat(sanitizador.tinhaMarcacao(null)).isFalse();
    }

    @Test
    @DisplayName("avisa quando havia marcacao, para a tentativa poder ser registrada")
    void avisaQuandoHaviaMarcacao() {
        assertThat(sanitizador.tinhaMarcacao("<script>alert(1)</script>")).isTrue();
        assertThat(sanitizador.tinhaMarcacao("Bolo de cenoura")).isFalse();
    }

    @Test
    @DisplayName("nao deixa entidade escapada virar marcacao de novo")
    void naoRemontaMarcacao() {
        String limpo = sanitizador.limpar("&lt;script&gt;alert(1)&lt;/script&gt;");

        assertThat(sanitizador.limpar(limpo)).doesNotContain("<script>");
    }
}
