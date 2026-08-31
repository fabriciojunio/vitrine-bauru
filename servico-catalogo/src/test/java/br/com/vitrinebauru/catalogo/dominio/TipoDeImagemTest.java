package br.com.vitrinebauru.catalogo.dominio;

import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tipo de imagem descoberto pelos bytes")
class TipoDeImagemTest {

    private static byte[] comCabecalho(int[] cabecalho, int tamanhoTotal) {
        byte[] arquivo = new byte[Math.max(tamanhoTotal, cabecalho.length)];
        for (int posicao = 0; posicao < cabecalho.length; posicao++) {
            arquivo[posicao] = (byte) cabecalho[posicao];
        }
        return arquivo;
    }

    private static byte[] jpeg() {
        return comCabecalho(new int[]{0xFF, 0xD8, 0xFF, 0xE0}, 64);
    }

    private static byte[] png() {
        return comCabecalho(new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, 64);
    }

    private static byte[] gif() {
        return comCabecalho(new int[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61}, 64);
    }

    private static byte[] webp() {
        byte[] arquivo = comCabecalho(new int[]{0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00}, 64);
        arquivo[8] = 'W';
        arquivo[9] = 'E';
        arquivo[10] = 'B';
        arquivo[11] = 'P';
        return arquivo;
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> imagensValidas() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("JPEG", jpeg(), TipoDeImagem.JPEG),
                org.junit.jupiter.params.provider.Arguments.of("PNG", png(), TipoDeImagem.PNG),
                org.junit.jupiter.params.provider.Arguments.of("GIF", gif(), TipoDeImagem.GIF),
                org.junit.jupiter.params.provider.Arguments.of("WEBP", webp(), TipoDeImagem.WEBP));
    }

    @ParameterizedTest(name = "reconhece {0}")
    @MethodSource("imagensValidas")
    @DisplayName("reconhece os quatro formatos aceitos")
    void reconheceFormatos(String nome, byte[] conteudo, TipoDeImagem esperado) {
        assertThat(TipoDeImagem.descobrir(conteudo)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "{0} devolve o tipo MIME certo")
    @MethodSource("imagensValidas")
    void tipoMimeCorreto(String nome, byte[] conteudo, TipoDeImagem esperado) {
        assertThat(TipoDeImagem.descobrir(conteudo).tipoMime())
                .isEqualTo("image/" + esperado.extensao().replace("jpg", "jpeg"));
    }

    @Test
    @DisplayName("recusa HTML disfarçado de foto, que é o ataque clássico de upload")
    void recusaHtmlDisfarcado() {
        byte[] paginaComScript = "<html><script>alert(1)</script></html>"
                .getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> TipoDeImagem.descobrir(paginaComScript))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class)
                .hasMessageContaining("não é uma imagem");
    }

    @Test
    @DisplayName("recusa executável, mesmo com nome de foto")
    void recusaExecutavel() {
        byte[] executavel = comCabecalho(new int[]{0x4D, 0x5A, 0x90, 0x00}, 128);

        assertThatThrownBy(() -> TipoDeImagem.descobrir(executavel))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class);
    }

    @Test
    @DisplayName("recusa PDF")
    void recusaPdf() {
        byte[] pdf = comCabecalho(new int[]{0x25, 0x50, 0x44, 0x46}, 64);

        assertThatThrownBy(() -> TipoDeImagem.descobrir(pdf))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class);
    }

    @Test
    @DisplayName("recusa arquivo RIFF que não é WEBP, como um áudio WAV")
    void recusaRiffQueNaoEWebp() {
        byte[] wav = comCabecalho(new int[]{0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00}, 64);
        wav[8] = 'W';
        wav[9] = 'A';
        wav[10] = 'V';
        wav[11] = 'E';

        assertThatThrownBy(() -> TipoDeImagem.descobrir(wav))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class);
    }

    @Test
    @DisplayName("recusa arquivo vazio")
    void recusaVazio() {
        assertThatThrownBy(() -> TipoDeImagem.descobrir(new byte[0]))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class)
                .hasMessageContaining("vazio");

        assertThatThrownBy(() -> TipoDeImagem.descobrir(null))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class);
    }

    @Test
    @DisplayName("recusa arquivo acima de 5 MB, com mensagem que ensina o que fazer")
    void recusaGrandeDemais() {
        byte[] gigante = new byte[TipoDeImagem.TAMANHO_MAXIMO + 1];
        gigante[0] = (byte) 0xFF;
        gigante[1] = (byte) 0xD8;
        gigante[2] = (byte) 0xFF;

        assertThatThrownBy(() -> TipoDeImagem.descobrir(gigante))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    @DisplayName("aceita arquivo exatamente no limite")
    void aceitaNoLimite() {
        byte[] noLimite = new byte[TipoDeImagem.TAMANHO_MAXIMO];
        noLimite[0] = (byte) 0xFF;
        noLimite[1] = (byte) 0xD8;
        noLimite[2] = (byte) 0xFF;

        assertThat(TipoDeImagem.descobrir(noLimite)).isEqualTo(TipoDeImagem.JPEG);
    }

    @Test
    @DisplayName("recusa arquivo curto demais para ter assinatura")
    void recusaCurtoDemais() {
        assertThatThrownBy(() -> TipoDeImagem.descobrir(new byte[]{(byte) 0xFF}))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class);
    }

    @Test
    @DisplayName("não confia na extensão, só nos bytes")
    void naoConfiaNaExtensao() {
        byte[] textoComNomeDeFoto = "isto aqui é texto puro".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> TipoDeImagem.descobrir(textoComNomeDeFoto))
                .isInstanceOf(ErrosDeNegocio.RegraDeNegocio.class);
    }
}
