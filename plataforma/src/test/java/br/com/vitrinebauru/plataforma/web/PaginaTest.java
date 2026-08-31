package br.com.vitrinebauru.plataforma.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Página da API")
class PaginaTest {

    @Test
    @DisplayName("converte a página do Spring Data no formato da API")
    void converte() {
        var pagina = new PageImpl<>(List.of("Bolo", "Pastel"), PageRequest.of(0, 2), 5);

        Pagina<String> resultado = Pagina.de(pagina, item -> item.toUpperCase());

        assertThat(resultado.conteudo()).containsExactly("BOLO", "PASTEL");
        assertThat(resultado.pagina()).isZero();
        assertThat(resultado.tamanho()).isEqualTo(2);
        assertThat(resultado.total()).isEqualTo(5);
        assertThat(resultado.totalDePaginas()).isEqualTo(3);
        assertThat(resultado.temProxima()).isTrue();
    }

    @Test
    @DisplayName("marca a ultima página como sem proxima")
    void ultimaPagina() {
        var pagina = new PageImpl<>(List.of("Pastel"), PageRequest.of(2, 2), 5);

        assertThat(Pagina.de(pagina, item -> item).temProxima()).isFalse();
    }

    @Test
    @DisplayName("página vazia mantem o tamanho pedido")
    void paginaVazia() {
        Pagina<String> vazia = Pagina.vazia(20);

        assertThat(vazia.conteudo()).isEmpty();
        assertThat(vazia.total()).isZero();
        assertThat(vazia.tamanho()).isEqualTo(20);
        assertThat(vazia.temProxima()).isFalse();
    }
}
