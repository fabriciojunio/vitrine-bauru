package br.com.vitrinebauru.notificacoes.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O texto do e-mail é parte do produto, e não detalhe de implementação: é o
 * único contato da plataforma com o empreendedor fora da tela. Estes testes
 * seguram o que não pode se perder numa alteração de texto.
 */
@DisplayName("Mensagens de e-mail")
class MensagensTest {

    static Stream<Mensagens.Conteudo> todasAsMensagens() {
        return Stream.of(
                Mensagens.boasVindas("Maria de Lourdes", "Doces da Lourdes"),
                Mensagens.aprovado("Maria de Lourdes", "Doces da Lourdes"),
                Mensagens.rejeitado("Maria de Lourdes", "Doces da Lourdes",
                        "O documento não confere com o nome do negócio."),
                Mensagens.suspenso("Maria de Lourdes", "Doces da Lourdes",
                        "Denúncia de propaganda enganosa em análise."));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todasAsMensagens")
    @DisplayName("toda mensagem tem assunto e corpo preenchidos")
    void temAssuntoECorpo(Mensagens.Conteudo conteudo) {
        assertThat(conteudo.assunto()).isNotBlank();
        assertThat(conteudo.corpo()).isNotBlank();
        assertThat(conteudo.assunto()).hasSizeLessThan(160);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todasAsMensagens")
    @DisplayName("toda mensagem chama a pessoa pelo nome")
    void chamaPeloNome(Mensagens.Conteudo conteudo) {
        assertThat(conteudo.corpo()).contains("Maria de Lourdes");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todasAsMensagens")
    @DisplayName("toda mensagem diz o nome do negócio")
    void dizONomeDoNegocio(Mensagens.Conteudo conteudo) {
        assertThat(conteudo.corpo()).contains("Doces da Lourdes");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todasAsMensagens")
    @DisplayName("toda mensagem assina como SEDECON, que é em quem a pessoa confia")
    void assinaComoSedecon(Mensagens.Conteudo conteudo) {
        assertThat(conteudo.corpo()).contains("SEDECON");
        assertThat(conteudo.corpo()).contains("Casa do Empreendedor");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todasAsMensagens")
    @DisplayName("nenhuma mensagem tem marcação HTML")
    void semHtml(Mensagens.Conteudo conteudo) {
        assertThat(conteudo.corpo()).doesNotContain("<");
        assertThat(conteudo.corpo()).doesNotContain("&nbsp;");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todasAsMensagens")
    @DisplayName("nenhuma mensagem tem jargão de sistema")
    void semJargao(Mensagens.Conteudo conteudo) {
        assertThat(conteudo.corpo().toLowerCase())
                .doesNotContain("processado com sucesso")
                .doesNotContain("erro")
                .doesNotContain("status")
                .doesNotContain("null");
    }

    @Test
    @DisplayName("a recusa leva o motivo escrito pela análise, inteiro")
    void recusaLevaOMotivo() {
        String motivo = "A descrição não explica o que você vende. Detalhe os produtos.";

        var conteudo = Mensagens.rejeitado("João", "Açaí do João", motivo);

        assertThat(conteudo.corpo()).contains(motivo);
    }

    @Test
    @DisplayName("a recusa diz que dá para corrigir e reenviar")
    void recusaDizComoCorrigir() {
        var conteudo = Mensagens.rejeitado("João", "Açaí do João", "Falta foto.");

        assertThat(conteudo.corpo()).contains("corrija");
        assertThat(conteudo.corpo()).contains("análise");
        assertThat(conteudo.corpo()).doesNotContain("cadastro cancelado");
    }

    @Test
    @DisplayName("a aprovação ensina o próximo passo, que é cadastrar foto")
    void aprovacaoEnsinaOProximoPasso() {
        var conteudo = Mensagens.aprovado("João", "Açaí do João");

        assertThat(conteudo.corpo()).contains("foto");
        assertThat(conteudo.assunto()).contains("no ar");
    }

    @Test
    @DisplayName("as boas-vindas explicam que ainda falta a análise")
    void boasVindasExplicamAFila() {
        var conteudo = Mensagens.boasVindas("João", "Açaí do João");

        assertThat(conteudo.corpo()).contains("análise");
        assertThat(conteudo.corpo()).contains("já pode entrar");
    }

    @Test
    @DisplayName("a suspensão avisa que os produtos continuam salvos")
    void suspensaoTranquilizaSobreOsProdutos() {
        var conteudo = Mensagens.suspenso("João", "Açaí do João", "Denúncia em análise.");

        assertThat(conteudo.corpo()).contains("continuam salvos");
    }

    @Test
    @DisplayName("os acentos estão corretos, porque é o que a pessoa vai ler")
    void acentosCorretos() {
        var conteudo = Mensagens.aprovado("João", "Açaí do João");

        assertThat(conteudo.corpo()).contains("João");
        assertThat(conteudo.corpo()).contains("Açaí");
        assertThat(conteudo.corpo()).contains("está");
    }
}
