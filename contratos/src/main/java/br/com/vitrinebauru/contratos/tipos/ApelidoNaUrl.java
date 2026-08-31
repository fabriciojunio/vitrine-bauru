package br.com.vitrinebauru.contratos.tipos;

import java.text.Normalizer;

/**
 * O pedaço do endereço que identifica a loja: /loja/doces-da-dona-lourdes.
 *
 * <p>É gerado a partir do nome do negócio para o empreendedor não precisar
 * entender o que é uma URL. Acento sai, cedilha vira c, espaço vira hífen, e o
 * resultado é conferido: se o nome for só símbolo, o apelido sairia vazio, e
 * aí o erro estoura aqui em vez de a loja nascer com endereço em branco.
 */
public record ApelidoNaUrl(String valor) {

    private static final int TAMANHO_MAXIMO = 60;

    public ApelidoNaUrl {
        if (valor == null || !valor.matches("[a-z0-9]+(-[a-z0-9]+)*")) {
            throw new ApelidoInvalido("Apelido só aceita letras minúsculas, números e hífen");
        }
        if (valor.length() > TAMANHO_MAXIMO) {
            throw new ApelidoInvalido("Apelido passa de " + TAMANHO_MAXIMO + " caracteres");
        }
    }

    public static ApelidoNaUrl deTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ApelidoInvalido("Informe o nome do negócio");
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String apelido = semAcento.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (apelido.length() > TAMANHO_MAXIMO) {
            apelido = apelido.substring(0, TAMANHO_MAXIMO).replaceAll("-+$", "");
        }
        if (apelido.isEmpty()) {
            throw new ApelidoInvalido("O nome do negócio precisa ter ao menos uma letra ou número");
        }
        return new ApelidoNaUrl(apelido);
    }

    /** Acrescenta sufixo quando o apelido já existe: doces-da-lourdes-2. */
    public ApelidoNaUrl comSufixo(int numero) {
        String base = valor;
        String sufixo = "-" + numero;
        if (base.length() + sufixo.length() > TAMANHO_MAXIMO) {
            base = base.substring(0, TAMANHO_MAXIMO - sufixo.length()).replaceAll("-+$", "");
        }
        return new ApelidoNaUrl(base + sufixo);
    }

    @Override
    public String toString() {
        return valor;
    }

    public static class ApelidoInvalido extends IllegalArgumentException {
        public ApelidoInvalido(String mensagem) {
            super(mensagem);
        }
    }
}
