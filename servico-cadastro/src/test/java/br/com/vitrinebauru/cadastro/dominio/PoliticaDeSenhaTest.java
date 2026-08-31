package br.com.vitrinebauru.cadastro.dominio;

import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Política de senha")
class PoliticaDeSenhaTest {

    private static final String EMAIL = "lourdes@exemplo.com";
    private static final String NOME = "Maria de Lourdes Prado";

    @ParameterizedTest(name = "aceita \"{0}\"")
    @ValueSource(strings = {
            "bolodepote2026",
            "minha senha longa",
            "Xk9#mQ2z",
            "bolodefuba2026",
            "12345678a",
            "vilacardia14",
            "arroz feijao bife batata"
    })
    @DisplayName("aceita senha longa o bastante e que não é óbvia")
    void aceitaSenhaBoa(String senha) {
        assertThatCode(() -> PoliticaDeSenha.exigirValida(senha, EMAIL, NOME))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "recusa \"{0}\"")
    @ValueSource(strings = {"", " ", "abc", "1234567", "senha"})
    @DisplayName("recusa senha curta")
    void recusaCurta(String senha) {
        assertThatThrownBy(() -> PoliticaDeSenha.exigirValida(senha, EMAIL, NOME))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class);
    }

    @Test
    @DisplayName("recusa senha nula")
    void recusaNula() {
        assertThatThrownBy(() -> PoliticaDeSenha.exigirValida(null, EMAIL, NOME))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class)
                .hasMessageContaining("8 caracteres");
    }

    @ParameterizedTest(name = "recusa \"{0}\"")
    @ValueSource(strings = {
            "12345678", "123456789", "1234567890", "senha123", "password",
            "qwertyui", "11111111", "abcd1234", "sedecon123", "bauru123", "vitrine123"
    })
    @DisplayName("recusa as senhas que todo mundo tenta primeiro")
    void recusaSenhaConhecida(String senha) {
        assertThatThrownBy(() -> PoliticaDeSenha.exigirValida(senha, EMAIL, NOME))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class);
    }

    @ParameterizedTest(name = "recusa \"{0}\"")
    @ValueSource(strings = {"aaaaaaaa", "00000000", "zzzzzzzzzz"})
    @DisplayName("recusa senha de um caractere só repetido")
    void recusaRepetida(String senha) {
        assertThatThrownBy(() -> PoliticaDeSenha.exigirValida(senha, EMAIL, NOME))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class)
                .hasMessageContaining("repetido");
    }

    @ParameterizedTest(name = "recusa \"{0}\"")
    @ValueSource(strings = {"lourdes123", "LOURDES2026", "mariadelourdes", "lourdesprado"})
    @DisplayName("recusa senha que é o próprio nome ou e-mail")
    void recusaSenhaComOsDados(String senha) {
        assertThatThrownBy(() -> PoliticaDeSenha.exigirValida(senha, EMAIL, NOME))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class)
                .hasMessageContaining("nome");
    }

    @Test
    @DisplayName("ignora acento ao comparar com o nome")
    void ignoraAcentoNaComparacao() {
        assertThatThrownBy(() -> PoliticaDeSenha.exigirValida(
                "joaosilva2026", "joao@exemplo.com", "João da Silva"))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class);
    }

    @Test
    @DisplayName("recusa senha maior que o limite do bcrypt")
    void recusaLongaDemais() {
        assertThatThrownBy(() -> PoliticaDeSenha.exigirValida("a1".repeat(40), EMAIL, NOME))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class)
                .hasMessageContaining("máximo");
    }

    @Test
    @DisplayName("aceita senha exatamente no limite de 72 bytes")
    void aceitaNoLimite() {
        assertThatCode(() -> PoliticaDeSenha.exigirValida("ab".repeat(36), EMAIL, NOME))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("não exige símbolo nem maiúscula, de propósito")
    void naoExigeComposicao() {
        assertThatCode(() -> PoliticaDeSenha.exigirValida("caderneta", EMAIL, NOME))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("funciona sem nome e sem e-mail informados")
    void funcionaSemDados() {
        assertThatCode(() -> PoliticaDeSenha.exigirValida("bolodepote2026", null, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("nome curto não bloqueia senha que só contém essas letras")
    void nomeCurtoNaoBloqueia() {
        assertThatCode(() -> PoliticaDeSenha.exigirValida("anaconda123", "ana@exemplo.com", "Ana"))
                .doesNotThrowAnyException();
    }
}
