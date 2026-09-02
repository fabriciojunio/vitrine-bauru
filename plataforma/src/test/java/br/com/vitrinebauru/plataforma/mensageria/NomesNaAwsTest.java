package br.com.vitrinebauru.plataforma.mensageria;

import br.com.vitrinebauru.contratos.Topicos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O nome que vai para a AWS.
 *
 * <p>Vale um teste por ser o tipo de coisa que falha em silêncio: o tópico é
 * criado com um nome, a fila assina outro, nada dá erro e a mensagem não
 * chega. Aqui a incoerência aparece no build.
 */
@DisplayName("Nomes na AWS")
class NomesNaAwsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            Topicos.EMPREENDEDORES,
            Topicos.CATALOGO,
            Topicos.CONTATOS,
            Topicos.PRIVACIDADE})
    @DisplayName("nenhum tópico do sistema vira nome que a AWS recusa")
    void topicoVirouNomeValido(String topico) {
        assertThat(NomesNaAws.doTopico(topico)).matches("[A-Za-z0-9_-]{1,256}");
    }

    @Test
    @DisplayName("o ponto do nome do tópico vira hífen, que é o que SNS e SQS aceitam")
    void pontoViraHifen() {
        assertThat(NomesNaAws.doTopico("vitrine.empreendedores")).isEqualTo("vitrine-empreendedores");
    }

    @Test
    @DisplayName("a fila leva o nome do grupo, para uma instância a mais dividir a mesma fila")
    void filaPorGrupo() {
        assertThat(NomesNaAws.daFila("vitrine-busca")).isEqualTo("vitrine-busca");
    }

    @Test
    @DisplayName("grupo que já começa com o nome do projeto não ganha o prefixo de novo")
    void naoDuplicaOPrefixo() {
        assertThat(NomesNaAws.daFila("vitrine-busca")).doesNotContain("vitrine-vitrine");
    }

    @ParameterizedTest
    @ValueSource(strings = {"vitrine-busca", "vitrine-catalogo", "vitrine-notificacoes"})
    @DisplayName("todo grupo real vira nome de fila que a AWS aceita")
    void grupoVirouFilaValida(String grupo) {
        assertThat(NomesNaAws.daFila(grupo)).matches("[A-Za-z0-9_-]{1,80}");
        assertThat(NomesNaAws.daFilaMorta(grupo)).matches("[A-Za-z0-9_-]{1,80}");
    }

    @Test
    @DisplayName("a fila morta fica ao lado da fila na listagem, com o mesmo começo")
    void filaMortaAoLado() {
        assertThat(NomesNaAws.daFilaMorta("vitrine-busca")).startsWith(NomesNaAws.daFila("vitrine-busca"));
    }

    @Test
    @DisplayName("fila e fila morta nunca colidem")
    void filaEFilaMortaSaoDiferentes() {
        assertThat(NomesNaAws.daFilaMorta("vitrine-busca")).isNotEqualTo(NomesNaAws.daFila("vitrine-busca"));
    }
}
