package br.com.vitrinebauru.plataforma.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Resposta paginada da API.
 *
 * <p>E um tipo do projeto, e nao o {@code Page} do Spring Data, porque o
 * {@code Page} serializa uma dezena de campos internos (pageable, sort,
 * numberOfElements) que viram contrato publico sem ninguem ter decidido isso.
 * Aqui o formato e escolhido e pequeno.
 */
public record Pagina<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long total,
        int totalDePaginas,
        boolean temProxima) {

    public static <O, D> Pagina<D> de(Page<O> pagina, Function<O, D> conversao) {
        return new Pagina<>(
                pagina.getContent().stream().map(conversao).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.hasNext());
    }

    public static <T> Pagina<T> vazia(int tamanho) {
        return new Pagina<>(List.of(), 0, tamanho, 0, 0, false);
    }
}
