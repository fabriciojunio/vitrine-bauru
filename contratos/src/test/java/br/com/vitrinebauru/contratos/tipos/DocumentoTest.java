package br.com.vitrinebauru.contratos.tipos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Os documentos usados aqui são números de teste conhecidos, escolhidos por
 * fecharem o dígito verificador, e não documentos de pessoas reais.
 */
@DisplayName("Documento (CPF e CNPJ)")
class DocumentoTest {

    @Nested
    @DisplayName("CPF")
    class Cpf {

        @Test
        @DisplayName("aceita CPF com dígito verificador correto")
        void aceitaCpfValido() {
            Documento documento = Documento.de("529.982.247-25");

            assertThat(documento.tipo()).isEqualTo(Documento.Tipo.CPF);
            assertThat(documento.valor()).isEqualTo("52998224725");
            assertThat(documento.ehCnpj()).isFalse();
        }

        @Test
        @DisplayName("recusa CPF com dígito trocado")
        void recusaCpfComDigitoTrocado() {
            assertThatThrownBy(() -> Documento.de("529.982.247-26"))
                    .isInstanceOf(Documento.DocumentoInvalido.class)
                    .hasMessageContaining("CPF inválido");
        }

        @ParameterizedTest(name = "recusa {0}")
        @ValueSource(strings = {"00000000000", "11111111111", "99999999999"})
        @DisplayName("recusa CPF com todos os dígitos iguais")
        void recusaDigitosRepetidos(String cpf) {
            assertThatThrownBy(() -> Documento.de(cpf))
                    .isInstanceOf(Documento.DocumentoInvalido.class);
        }

        @Test
        @DisplayName("formata para leitura e mascara para exibicao")
        void formataEMascara() {
            Documento documento = Documento.de("52998224725");

            assertThat(documento.formatado()).isEqualTo("529.982.247-25");
            assertThat(documento.mascarado()).isEqualTo("***.982.247-**");
        }
    }

    @Nested
    @DisplayName("CNPJ numerico")
    class CnpjNumerico {

        @Test
        @DisplayName("aceita CNPJ com dígito verificador correto")
        void aceitaCnpjValido() {
            Documento documento = Documento.de("11.222.333/0001-81");

            assertThat(documento.tipo()).isEqualTo(Documento.Tipo.CNPJ);
            assertThat(documento.valor()).isEqualTo("11222333000181");
            assertThat(documento.ehCnpj()).isTrue();
            assertThat(documento.formatado()).isEqualTo("11.222.333/0001-81");
            assertThat(documento.mascarado()).isEqualTo("**.222.333/****-**");
        }

        @Test
        @DisplayName("recusa CNPJ com dígito trocado")
        void recusaCnpjComDigitoTrocado() {
            assertThatThrownBy(() -> Documento.de("11.222.333/0001-82"))
                    .isInstanceOf(Documento.DocumentoInvalido.class)
                    .hasMessageContaining("CNPJ inválido");
        }
    }

    @Nested
    @DisplayName("CNPJ alfanumérico, emitido a partir de julho de 2026")
    class CnpjAlfanumerico {

        @Test
        @DisplayName("aceita o exemplo oficial 12.ABC.345/01DE-35")
        void aceitaExemploOficial() {
            Documento documento = Documento.de("12.ABC.345/01DE-35");

            assertThat(documento.tipo()).isEqualTo(Documento.Tipo.CNPJ);
            assertThat(documento.valor()).isEqualTo("12ABC34501DE35");
            assertThat(documento.formatado()).isEqualTo("12.ABC.345/01DE-35");
        }

        @Test
        @DisplayName("aceita letra minuscula e normaliza para maiúscula")
        void normalizaCaixa() {
            assertThat(Documento.de("12abc34501de35").valor()).isEqualTo("12ABC34501DE35");
        }

        @Test
        @DisplayName("recusa letra nos dois dígitos verificadores")
        void recusaLetraNoDigitoVerificador() {
            assertThatThrownBy(() -> Documento.de("12ABC34501DEAB"))
                    .isInstanceOf(Documento.DocumentoInvalido.class)
                    .hasMessageContaining("dígitos verificadores");
        }

        @Test
        @DisplayName("recusa alfanumérico com dígito verificador errado")
        void recusaDigitoErrado() {
            assertThatThrownBy(() -> Documento.de("12ABC34501DE34"))
                    .isInstanceOf(Documento.DocumentoInvalido.class)
                    .hasMessageContaining("CNPJ inválido");
        }
    }

    @Nested
    @DisplayName("entrada malformada")
    class EntradaMalformada {

        @ParameterizedTest(name = "recusa \"{0}\"")
        @ValueSource(strings = {"123", "1234567890123456789", "abc"})
        void recusaTamanhoErrado(String bruto) {
            assertThatThrownBy(() -> Documento.de(bruto))
                    .isInstanceOf(Documento.DocumentoInvalido.class)
                    .hasMessageContaining("11 dígitos");
        }

        @Test
        void recusaVazio() {
            assertThatThrownBy(() -> Documento.de("  "))
                    .isInstanceOf(Documento.DocumentoInvalido.class)
                    .hasMessageContaining("Informe");
        }

        @Test
        void recusaNulo() {
            assertThatThrownBy(() -> Documento.de(null))
                    .isInstanceOf(Documento.DocumentoInvalido.class);
        }

        @Test
        @DisplayName("recusa letra dentro de um documento de 11 caracteres, que só pode ser CPF")
        void recusaLetraEmCpf() {
            assertThatThrownBy(() -> Documento.de("5299822472A"))
                    .isInstanceOf(Documento.DocumentoInvalido.class)
                    .hasMessageContaining("CPF só pode ter números");
        }
    }
}
