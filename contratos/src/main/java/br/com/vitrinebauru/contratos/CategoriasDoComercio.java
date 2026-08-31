package br.com.vitrinebauru.contratos;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

/**
 * Ramos de atividade aceitos.
 *
 * <p>São poucos e largos de propósito. Uma lista com quarenta categorias fica
 * mais precisa no papel e, na prática, faz o empreendedor parar no meio do
 * cadastro tentando decidir entre "Doces e bolos" e "Alimentação artesanal", e
 * faz o consumidor não achar nada porque procurou na categoria vizinha.
 */
public final class CategoriasDoComercio {

    private static final List<String> CATEGORIAS = List.of(
            "Alimentação",
            "Artesanato",
            "Beleza e bem-estar",
            "Casa e construção",
            "Moda e acessórios",
            "Serviços gerais",
            "Assistência técnica",
            "Educação e aulas",
            "Pet",
            "Saúde",
            "Eventos e festas",
            "Automotivo");

    private CategoriasDoComercio() {
    }

    public static List<String> todas() {
        return CATEGORIAS;
    }

    public static boolean existe(String categoria) {
        return normalizada(categoria).isPresent();
    }

    public static Optional<String> normalizada(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return Optional.empty();
        }
        String procurada = semAcento(categoria);
        return CATEGORIAS.stream()
                .filter(oficial -> semAcento(oficial).equals(procurada))
                .findFirst();
    }

    private static String semAcento(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
