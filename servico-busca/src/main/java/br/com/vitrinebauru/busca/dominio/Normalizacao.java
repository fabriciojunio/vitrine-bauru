package br.com.vitrinebauru.busca.dominio;

import java.text.Normalizer;

/**
 * Deixa o texto pronto para comparar.
 *
 * <p>Quem procura "acai" precisa achar "Açaí", e quem procura "PASTEL" precisa
 * achar "Pastel de feira". A normalizacao acontece nos dois lados: no texto
 * guardado, quando o evento chega, e no termo digitado, na hora da busca.
 *
 * <p>Fazer isso em Java, e nao com a extensao {@code unaccent} do PostgreSQL,
 * e escolha consciente. A extensao seria mais elegante e amarraria a busca a
 * um recurso que nem toda hospedagem gratuita habilita. Com o texto ja
 * normalizado na gravacao, a consulta e um {@code like} comum que funciona em
 * qualquer PostgreSQL.
 *
 * <p>Na escala deste projeto (algumas centenas de lojas de um municipio), o
 * {@code like} varre a tabela em milissegundos. Quando isso deixar de ser
 * verdade, o passo seguinte e um indice GIN com {@code pg_trgm} sobre esta
 * mesma coluna, sem mexer em mais nada.
 */
public final class Normalizacao {

    private Normalizacao() {
    }

    public static String paraBusca(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Junta os campos que a busca por palavra precisa varrer. */
    public static String juntar(String... partes) {
        StringBuilder tudo = new StringBuilder();
        for (String parte : partes) {
            if (parte != null && !parte.isBlank()) {
                tudo.append(paraBusca(parte)).append(' ');
            }
        }
        return tudo.toString().trim();
    }
}
