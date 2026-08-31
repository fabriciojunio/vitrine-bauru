package br.com.vitrinebauru.contratos.tipos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validação de documento sobre uma amostra gerada, e não sobre três exemplos
 * escolhidos a dedo.
 *
 * <p>Testar CPF com dois ou três casos escritos à mão dá a sensação de estar
 * coberto e não está: o erro típico do módulo 11 aparece em faixas
 * específicas, como quando o resto é 0 ou 1 e o dígito precisa virar zero. A
 * amostra aqui é gerada percorrendo essas faixas, e cada número é conferido
 * pelo caminho inverso: o gerador calcula o dígito, e a validação precisa
 * concordar.
 *
 * <p>Os números são sintéticos. Nenhum documento de pessoa real entra em teste.
 */
@DisplayName("Documento sobre amostra gerada")
class DocumentoGeradoTest {

    private static final int[] PESOS_CPF_PRIMEIRO = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CPF_SEGUNDO = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_PRIMEIRO = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_SEGUNDO = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private static int digito(String base, int[] pesos) {
        int soma = 0;
        for (int posicao = 0; posicao < base.length(); posicao++) {
            soma += (base.charAt(posicao) - 48) * pesos[posicao];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static String cpfDe(long base) {
        String nove = String.format("%09d", Math.abs(base) % 1_000_000_000L);
        int primeiro = digito(nove, PESOS_CPF_PRIMEIRO);
        int segundo = digito(nove + primeiro, PESOS_CPF_SEGUNDO);
        return nove + primeiro + segundo;
    }

    private static String cnpjDe(long base) {
        String doze = String.format("%012d", Math.abs(base) % 1_000_000_000_000L);
        int primeiro = digito(doze, PESOS_CNPJ_PRIMEIRO);
        int segundo = digito(doze + primeiro, PESOS_CNPJ_SEGUNDO);
        return doze + primeiro + segundo;
    }

    /** Percorre faixas diferentes do módulo 11, inclusive as que dão resto 0 e 1. */
    static Stream<String> cpfsValidos() {
        return IntStream.rangeClosed(1, 25)
                .mapToObj(indice -> cpfDe(12_345_678L * indice + indice))
                .filter(cpf -> cpf.chars().distinct().count() > 1);
    }

    static Stream<String> cnpjsValidos() {
        return IntStream.rangeClosed(1, 25)
                .mapToObj(indice -> cnpjDe(11_222_333_000_1L * indice + indice))
                .filter(cnpj -> cnpj.chars().distinct().count() > 1);
    }

    static Stream<String> cpfsComDigitoTrocado() {
        return cpfsValidos().map(cpf -> {
            char ultimo = cpf.charAt(10);
            char trocado = ultimo == '9' ? '0' : (char) (ultimo + 1);
            return cpf.substring(0, 10) + trocado;
        });
    }

    static Stream<String> cnpjsComDigitoTrocado() {
        return cnpjsValidos().map(cnpj -> {
            char ultimo = cnpj.charAt(13);
            char trocado = ultimo == '9' ? '0' : (char) (ultimo + 1);
            return cnpj.substring(0, 13) + trocado;
        });
    }

    @ParameterizedTest(name = "aceita o CPF {0}")
    @MethodSource("cpfsValidos")
    @DisplayName("aceita todo CPF cujo dígito verificador fecha")
    void aceitaCpfValido(String cpf) {
        Documento documento = Documento.de(cpf);

        assertThat(documento.tipo()).isEqualTo(Documento.Tipo.CPF);
        assertThat(documento.valor()).isEqualTo(cpf);
    }

    @ParameterizedTest(name = "recusa o CPF {0}")
    @MethodSource("cpfsComDigitoTrocado")
    @DisplayName("recusa quando um dígito é trocado, que é o erro de digitação típico")
    void recusaCpfComDigitoTrocado(String cpf) {
        assertThatThrownBy(() -> Documento.de(cpf))
                .isInstanceOf(Documento.DocumentoInvalido.class);
    }

    @ParameterizedTest(name = "aceita o CNPJ {0}")
    @MethodSource("cnpjsValidos")
    @DisplayName("aceita todo CNPJ cujo dígito verificador fecha")
    void aceitaCnpjValido(String cnpj) {
        Documento documento = Documento.de(cnpj);

        assertThat(documento.tipo()).isEqualTo(Documento.Tipo.CNPJ);
        assertThat(documento.ehCnpj()).isTrue();
    }

    @ParameterizedTest(name = "recusa o CNPJ {0}")
    @MethodSource("cnpjsComDigitoTrocado")
    @DisplayName("recusa CNPJ com dígito trocado")
    void recusaCnpjComDigitoTrocado(String cnpj) {
        assertThatThrownBy(() -> Documento.de(cnpj))
                .isInstanceOf(Documento.DocumentoInvalido.class);
    }

    @ParameterizedTest(name = "{0} formatado e lido de volta continua o mesmo")
    @MethodSource("cpfsValidos")
    @DisplayName("formatar e ler de volta não perde nem muda nada")
    void idaEVoltaDoCpf(String cpf) {
        Documento original = Documento.de(cpf);

        Documento relido = Documento.de(original.formatado());

        assertThat(relido).isEqualTo(original);
    }

    @ParameterizedTest(name = "a máscara de {0} esconde o suficiente")
    @MethodSource("cpfsValidos")
    @DisplayName("o documento mascarado esconde o começo e o fim, e mostra só o miolo")
    void mascaraEsconde(String cpf) {
        String mascarado = Documento.de(cpf).mascarado();

        // A verificação é sobre a forma, e não sobre a ausência dos dígitos:
        // um CPF pode repetir no miolo os mesmos três dígitos do começo, e
        // isso é coincidência, não vazamento.
        assertThat(mascarado).matches("\\*\\*\\*\\.\\d{3}\\.\\d{3}-\\*\\*");
        assertThat(mascarado).contains(cpf.substring(3, 6) + "." + cpf.substring(6, 9));
        assertThat(mascarado).hasSameSizeAs("000.000.000-00");
    }
}
