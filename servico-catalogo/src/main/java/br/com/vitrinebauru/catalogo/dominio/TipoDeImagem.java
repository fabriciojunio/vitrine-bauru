package br.com.vitrinebauru.catalogo.dominio;

import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;

import java.util.Arrays;

/**
 * Descobre o tipo da imagem pelos primeiros bytes do arquivo.
 *
 * <p>A extensao do nome e o cabecalho enviado pelo navegador sao escritos por
 * quem esta enviando, entao nenhum dos dois prova nada. Um arquivo chamado
 * {@code foto.jpg}, anunciado como {@code image/jpeg}, pode ser um HTML com
 * script dentro; servido de volta no mesmo dominio da API, ele executaria no
 * navegador de quem abrir.
 *
 * <p>Os primeiros bytes, ao contrario, sao o formato de verdade. Este enum le
 * essa assinatura e recusa o que nao for uma das quatro imagens aceitas.
 */
public enum TipoDeImagem {

    JPEG("image/jpeg", "jpg", new int[]{0xFF, 0xD8, 0xFF}),
    PNG("image/png", "png", new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
    GIF("image/gif", "gif", new int[]{0x47, 0x49, 0x46, 0x38}),
    WEBP("image/webp", "webp", new int[]{0x52, 0x49, 0x46, 0x46});

    /** Cinco megabytes cobre foto de celular sem virar depósito de arquivo. */
    public static final int TAMANHO_MAXIMO = 5 * 1024 * 1024;

    private final String tipoMime;
    private final String extensao;
    private final int[] assinatura;

    TipoDeImagem(String tipoMime, String extensao, int[] assinatura) {
        this.tipoMime = tipoMime;
        this.extensao = extensao;
        this.assinatura = assinatura;
    }

    public String tipoMime() {
        return tipoMime;
    }

    public String extensao() {
        return extensao;
    }

    public static TipoDeImagem descobrir(byte[] conteudo) {
        if (conteudo == null || conteudo.length == 0) {
            throw new ErrosDeNegocio.RegraDeNegocio("O arquivo enviado está vazio.");
        }
        if (conteudo.length > TAMANHO_MAXIMO) {
            throw new ErrosDeNegocio.RegraDeNegocio(
                    "A imagem passa de 5 MB. Tire a foto em qualidade menor ou reduza o arquivo.");
        }

        return Arrays.stream(values())
                .filter(tipo -> tipo.combina(conteudo))
                .filter(tipo -> tipo != WEBP || ehWebpDeVerdade(conteudo))
                .findFirst()
                .orElseThrow(() -> new ErrosDeNegocio.RegraDeNegocio(
                        "Envie uma imagem JPG, PNG, GIF ou WEBP. O arquivo enviado não é uma imagem."));
    }

    private boolean combina(byte[] conteudo) {
        if (conteudo.length < assinatura.length) {
            return false;
        }
        for (int posicao = 0; posicao < assinatura.length; posicao++) {
            if ((conteudo[posicao] & 0xFF) != assinatura[posicao]) {
                return false;
            }
        }
        return true;
    }

    /**
     * RIFF sozinho tambem e o comeco de arquivo de audio. O que distingue o
     * WEBP e a palavra na posicao 8.
     */
    private static boolean ehWebpDeVerdade(byte[] conteudo) {
        if (conteudo.length < 12) {
            return false;
        }
        return conteudo[8] == 'W' && conteudo[9] == 'E' && conteudo[10] == 'B' && conteudo[11] == 'P';
    }
}
