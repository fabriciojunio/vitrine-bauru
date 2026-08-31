package br.com.vitrinebauru.busca.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Normalização para busca")
class NormalizacaoTest {

    @ParameterizedTest(name = "\"{0}\" vira \"{1}\"")
    @CsvSource({
            "'Açaí', 'acai'",
            "'AÇAÍ', 'acai'",
            "'Pastel de Feira', 'pastel de feira'",
            "'Conserto de Máquina de Lavar', 'conserto de maquina de lavar'",
            "'Bolo de Cenoura com Chocolate', 'bolo de cenoura com chocolate'",
            "'Tibiriçá', 'tibirica'",
            "'Núcleo Habitacional Mary Dota', 'nucleo habitacional mary dota'",
            "'Marmita  com    espaço  demais', 'marmita com espaço demais'",
            "'   sobrando nas pontas   ', 'sobrando nas pontas'",
            "'João', 'joao'",
            "'Móveis sob medida', 'moveis sob medida'"
    })
    @DisplayName("tira acento, baixa a caixa e junta espaço repetido")
    void normaliza(String original, String esperado) {
        assertThat(Normalizacao.paraBusca(original))
                .isEqualTo(Normalizacao.paraBusca(esperado));
    }

    @Test
    @DisplayName("quem procura sem acento encontra quem cadastrou com acento")
    void buscaSemAcentoEncontraComAcento() {
        String guardado = Normalizacao.juntar("Açaí do João", "Açaí no copo e na tigela",
                "Alimentação", "Vila Falcão");

        assertThat(guardado).contains(Normalizacao.paraBusca("acai"));
        assertThat(guardado).contains(Normalizacao.paraBusca("joao"));
        assertThat(guardado).contains(Normalizacao.paraBusca("falcao"));
    }

    @Test
    @DisplayName("quem procura com acento também encontra")
    void buscaComAcentoTambemEncontra() {
        String guardado = Normalizacao.juntar("Acai do Joao", "sem acento no cadastro");

        assertThat(guardado).contains(Normalizacao.paraBusca("Açaí"));
    }

    @Test
    @DisplayName("junta só o que existe, sem sobrar espaço de campo vazio")
    void juntaIgnorandoVazios() {
        String junto = Normalizacao.juntar("Bolo", null, "", "   ", "Alimentação");

        assertThat(junto).isEqualTo("bolo alimentacao");
    }

    @Test
    @DisplayName("texto nulo vira vazio, e não estoura")
    void nuloViraVazio() {
        assertThat(Normalizacao.paraBusca(null)).isEmpty();
        assertThat(Normalizacao.juntar((String) null)).isEmpty();
    }

    @Test
    @DisplayName("preserva número, que é o que distingue produto de tamanho diferente")
    void preservaNumero() {
        assertThat(Normalizacao.paraBusca("Marmita 500g")).isEqualTo("marmita 500g");
        assertThat(Normalizacao.paraBusca("Conserto 24h")).isEqualTo("conserto 24h");
    }
}
