package br.com.vitrinebauru.contratos.tipos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Telefone")
class TelefoneTest {

    @ParameterizedTest(name = "aceita \"{0}\"")
    @ValueSource(strings = {
            "(14) 99712-3456",
            "14997123456",
            "14 99712 3456",
            "+55 14 99712-3456",
            "5514997123456"
    })
    @DisplayName("aceita celular escrito de varios jeitos e normaliza")
    void aceitaVariasFormas(String bruto) {
        Telefone telefone = Telefone.de(bruto);

        assertThat(telefone.ddd()).isEqualTo("14");
        assertThat(telefone.numero()).isEqualTo("997123456");
        assertThat(telefone.somenteDigitos()).isEqualTo("14997123456");
    }

    @Test
    @DisplayName("monta o número no formato que o link do WhatsApp espera")
    void montaNumeroParaWhatsapp() {
        assertThat(Telefone.de("(14) 99712-3456").paraWhatsapp()).isEqualTo("5514997123456");
    }

    @Test
    @DisplayName("aceita telefone fixo de oito dígitos")
    void aceitaFixo() {
        Telefone fixo = Telefone.de("(14) 3227-7819");

        assertThat(fixo.ehCelular()).isFalse();
        assertThat(fixo.formatado()).isEqualTo("(14) 3227-7819");
    }

    @Test
    @DisplayName("formata celular com o nono dígito")
    void formataCelular() {
        assertThat(Telefone.de("14997123456").formatado()).isEqualTo("(14) 99712-3456");
    }

    @Test
    @DisplayName("recusa celular de nove dígitos que não começa com 9")
    void recusaNonoDigitoErrado() {
        assertThatThrownBy(() -> Telefone.de("14897123456"))
                .isInstanceOf(Telefone.TelefoneInvalido.class)
                .hasMessageContaining("começar com 9");
    }

    @ParameterizedTest(name = "recusa \"{0}\"")
    @ValueSource(strings = {"1234", "999999999999999", "0000000000"})
    @DisplayName("recusa número fora do formato brasileiro")
    void recusaForaDoFormato(String bruto) {
        assertThatThrownBy(() -> Telefone.de(bruto))
                .isInstanceOf(Telefone.TelefoneInvalido.class);
    }

    @Test
    void recusaVazio() {
        assertThatThrownBy(() -> Telefone.de(""))
                .isInstanceOf(Telefone.TelefoneInvalido.class)
                .hasMessageContaining("Informe");
    }
}
