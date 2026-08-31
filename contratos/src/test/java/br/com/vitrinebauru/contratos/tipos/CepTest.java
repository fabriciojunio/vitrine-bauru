package br.com.vitrinebauru.contratos.tipos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CEP")
class CepTest {

    @Test
    @DisplayName("aceita o CEP da Casa do Empreendedor, com e sem hífen")
    void aceitaComESemHifen() {
        assertThat(Cep.de("17011-066").valor()).isEqualTo("17011066");
        assertThat(Cep.de("17011066").valor()).isEqualTo("17011066");
    }

    @Test
    void formataComHifen() {
        assertThat(Cep.de("17011066").formatado()).isEqualTo("17011-066");
    }

    @ParameterizedTest(name = "recusa \"{0}\"")
    @ValueSource(strings = {"1701106", "170110666", "abcdefgh"})
    void recusaTamanhoErrado(String bruto) {
        assertThatThrownBy(() -> Cep.de(bruto))
                .isInstanceOf(Cep.CepInvalido.class)
                .hasMessageContaining("8 dígitos");
    }

    @Test
    void recusaVazio() {
        assertThatThrownBy(() -> Cep.de(null)).isInstanceOf(Cep.CepInvalido.class);
    }
}
