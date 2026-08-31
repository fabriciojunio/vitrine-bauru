package br.com.vitrinebauru.contratos.tipos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Apelido na URL")
class ApelidoNaUrlTest {

    @ParameterizedTest(name = "\"{0}\" vira \"{1}\"")
    @CsvSource({
            "'Doces da Dona Lourdes', 'doces-da-dona-lourdes'",
            "'Salgados & Cia', 'salgados-cia'",
            "'Açaí do João', 'acai-do-joao'",
            "'MARCENARIA IRMÃOS PEREIRA', 'marcenaria-irmaos-pereira'",
            "'  espaço  sobrando  ', 'espaco-sobrando'",
            "'Conserto 24h', 'conserto-24h'",
            "'Bolo-de-pote', 'bolo-de-pote'"
    })
    @DisplayName("tira acento, cedilha e simbolo do nome do negocio")
    void geraApelido(String nome, String esperado) {
        assertThat(ApelidoNaUrl.deTexto(nome).valor()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("corta nome muito longo sem terminar em hifen")
    void cortaNomeLongo() {
        String nomeLongo = "Cooperativa dos Produtores Artesanais da Regiao Central de Bauru e Adjacencias";

        ApelidoNaUrl apelido = ApelidoNaUrl.deTexto(nomeLongo);

        assertThat(apelido.valor()).hasSizeLessThanOrEqualTo(60);
        assertThat(apelido.valor()).doesNotEndWith("-");
    }

    @Test
    @DisplayName("acrescenta numero quando o apelido ja existe")
    void acrescentaSufixo() {
        ApelidoNaUrl apelido = ApelidoNaUrl.deTexto("Doces da Lourdes");

        assertThat(apelido.comSufixo(2).valor()).isEqualTo("doces-da-lourdes-2");
    }

    @Test
    @DisplayName("mantem o limite de tamanho mesmo com sufixo")
    void sufixoRespeitaLimite() {
        ApelidoNaUrl apelido = ApelidoNaUrl.deTexto("a".repeat(60));

        ApelidoNaUrl comSufixo = apelido.comSufixo(12);

        assertThat(comSufixo.valor()).hasSizeLessThanOrEqualTo(60);
        assertThat(comSufixo.valor()).endsWith("-12");
    }

    @Test
    @DisplayName("recusa nome que so tem simbolo, porque sobraria endereco vazio")
    void recusaNomeSoComSimbolo() {
        assertThatThrownBy(() -> ApelidoNaUrl.deTexto("!!! @@@ ###"))
                .isInstanceOf(ApelidoNaUrl.ApelidoInvalido.class)
                .hasMessageContaining("ao menos uma letra");
    }

    @Test
    @DisplayName("recusa apelido construido a mao fora do formato")
    void recusaFormatoInvalido() {
        assertThatThrownBy(() -> new ApelidoNaUrl("Doces Da Lourdes"))
                .isInstanceOf(ApelidoNaUrl.ApelidoInvalido.class);
        assertThatThrownBy(() -> new ApelidoNaUrl("-comeca-com-hifen"))
                .isInstanceOf(ApelidoNaUrl.ApelidoInvalido.class);
    }
}
