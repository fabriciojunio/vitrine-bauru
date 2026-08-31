package br.com.vitrinebauru.contratos.tipos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Dinheiro")
class DinheiroTest {

    @ParameterizedTest(name = "{0} centavos viram {1}")
    @CsvSource({
            "0, 'R$ 0,00'",
            "5, 'R$ 0,05'",
            "50, 'R$ 0,50'",
            "100, 'R$ 1,00'",
            "1250, 'R$ 12,50'",
            "100000, 'R$ 1.000,00'",
            "123456789, 'R$ 1.234.567,89'"
    })
    @DisplayName("formata em real com separador de milhar")
    void formata(long centavos, String esperado) {
        assertThat(Dinheiro.deCentavos(centavos).formatado()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("usa espaço comum, e não o espaço não separável do NumberFormat")
    void naoUsaEspacoEstranho() {
        String formatado = Dinheiro.deCentavos(1250).formatado();

        assertThat(formatado.chars().anyMatch(c -> c == 160))
                .as("o separador depois do R$ nao pode ser espaco nao separavel")
                .isFalse();
        assertThat(formatado.charAt(2)).isEqualTo(' ');
    }

    @ParameterizedTest(name = "\"{0}\" vira {1} centavos")
    @CsvSource({
            "'12,50', 1250",
            "'R$ 12,50', 1250",
            "'1.234,56', 123456",
            "'0,99', 99",
            "'10', 1000"
    })
    @DisplayName("lê valor digitado pelo empreendedor no formato brasileiro")
    void leValorDigitado(String digitado, long centavos) {
        assertThat(Dinheiro.deReais(digitado).centavos()).isEqualTo(centavos);
    }

    @Test
    @DisplayName("arredonda a terceira casa para o centavo mais proximo")
    void arredonda() {
        assertThat(Dinheiro.deReais("10,005").centavos()).isEqualTo(1001);
        assertThat(Dinheiro.deReais("10,004").centavos()).isEqualTo(1000);
    }

    @Test
    @DisplayName("soma sem erro de ponto flutuante")
    void soma() {
        Dinheiro total = Dinheiro.deReais("0,10")
                .mais(Dinheiro.deReais("0,20"))
                .mais(Dinheiro.deReais("24,00"));

        assertThat(total.centavos()).isEqualTo(2430);
        assertThat(total.formatado()).isEqualTo("R$ 24,30");
    }

    @Test
    @DisplayName("recusa preço negativo")
    void recusaNegativo() {
        assertThatThrownBy(() -> Dinheiro.deCentavos(-1))
                .isInstanceOf(Dinheiro.DinheiroInvalido.class)
                .hasMessageContaining("negativo");
    }

    @Test
    @DisplayName("recusa texto que não e número")
    void recusaTextoInvalido() {
        assertThatThrownBy(() -> Dinheiro.deReais("combinar"))
                .isInstanceOf(Dinheiro.DinheiroInvalido.class);
    }

    @Test
    @DisplayName("ordena por valor")
    void ordena() {
        assertThat(Dinheiro.deCentavos(100)).isLessThan(Dinheiro.deCentavos(200));
        assertThat(Dinheiro.ZERO.centavos()).isZero();
    }
}
