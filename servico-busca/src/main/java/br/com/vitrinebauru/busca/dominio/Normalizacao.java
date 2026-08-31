package br.com.vitrinebauru.busca.dominio;

import java.text.Normalizer;

/**
 * Deixa o texto pronto para comparar.
 *
 * <p>Quem procura "açaí" precisa achar "Açaí", e quem procura "PASTEL" precisa
 * achar "Pastel de feira". A normalização acontece nos dois lados: no texto
 * guardado, quando o evento chega, e no termo digitado, na hora da busca.
 *
 * <p>Fazer isso em Java, e não com a extensão {@code unaccent} do PostgreSQL,
 * é escolha consciente. A extensão seria mais elegante e amarraria a busca a
 * um recurso que nem toda hospedagem gratuita habilita. Com o texto já
 * normalizado na gravação, a consulta é um {@code like} comum que funciona em
 * qualquer PostgreSQL.
 *
 * <p>Na escala deste projeto (algumas centenas de lojas de um município), o
 * {@code like} varre a tabela em milissegundos. Quando isso deixar de ser
 * verdade, o passo seguinte é um índice GIN com {@code pg_trgm} sobre esta
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
