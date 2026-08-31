package br.com.vitrinebauru.contratos.tipos;

import java.text.Normalizer;

/**
 * O pedaco do endereco que identifica a loja: /loja/doces-da-dona-lourdes.
 *
 * <p>E gerado a partir do nome do negocio para o empreendedor nao precisar
 * entender o que e uma URL. Acento sai, cedilha vira c, espaco vira hifen, e o
 * resultado e conferido: se o nome for so simbolo, o apelido sairia vazio, e
 * ai o erro estoura aqui em vez de a loja nascer com endereco em branco.
 */
public record ApelidoNaUrl(String valor) {

    private static final int TAMANHO_MAXIMO = 60;

    public ApelidoNaUrl {
        if (valor == null || !valor.matches("[a-z0-9]+(-[a-z0-9]+)*")) {
            throw new ApelidoInvalido("Apelido so aceita letras minusculas, numeros e hifen");
        }
        if (valor.length() > TAMANHO_MAXIMO) {
            throw new ApelidoInvalido("Apelido passa de " + TAMANHO_MAXIMO + " caracteres");
        }
    }

    public static ApelidoNaUrl deTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ApelidoInvalido("Informe o nome do negocio");
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
            throw new ApelidoInvalido("O nome do negocio precisa ter ao menos uma letra ou numero");
        }
        return new ApelidoNaUrl(apelido);
    }

    /** Acrescenta sufixo quando o apelido ja existe: doces-da-lourdes-2. */
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
