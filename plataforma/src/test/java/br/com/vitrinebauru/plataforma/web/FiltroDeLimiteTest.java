package br.com.vitrinebauru.plataforma.web;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("Limite de requisições")
class FiltroDeLimiteTest {

    private static final String LOGIN = "/api/cadastro/auth/login";

    private FiltroDeLimite filtroCom(boolean ativo, int capacidade) {
        var propriedades = new PropriedadesDeLimite(ativo, List.of(
                new PropriedadesDeLimite.Regra("/api/cadastro/auth/**", capacidade, Duration.ofMinutes(1))));
        return new FiltroDeLimite(propriedades, new SimpleMeterRegistry());
    }

    private int tentar(FiltroDeLimite filtro, String caminho, String ip) throws Exception {
        var requisicao = new MockHttpServletRequest("POST", caminho);
        requisicao.setRemoteAddr(ip);
        var resposta = new MockHttpServletResponse();
        filtro.doFilter(requisicao, resposta, mock(FilterChain.class));
        return resposta.getStatus();
    }

    @Test
    @DisplayName("deixa passar até a capacidade e barra a proxima")
    void barraDepoisDaCapacidade() throws Exception {
        var filtro = filtroCom(true, 5);

        for (int tentativa = 1; tentativa <= 5; tentativa++) {
            assertThat(tentar(filtro, LOGIN, "200.100.50.1")).isEqualTo(200);
        }

        assertThat(tentar(filtro, LOGIN, "200.100.50.1")).isEqualTo(429);
    }

    @Test
    @DisplayName("conta separado por endereço de origem")
    void contaSeparadoPorOrigem() throws Exception {
        var filtro = filtroCom(true, 2);

        tentar(filtro, LOGIN, "200.100.50.1");
        tentar(filtro, LOGIN, "200.100.50.1");

        assertThat(tentar(filtro, LOGIN, "200.100.50.1")).isEqualTo(429);
        assertThat(tentar(filtro, LOGIN, "200.100.50.2")).isEqualTo(200);
    }

    @Test
    @DisplayName("não limita caminho fora das regras")
    void naoLimitaOutroCaminho() throws Exception {
        var filtro = filtroCom(true, 1);

        tentar(filtro, LOGIN, "200.100.50.1");

        for (int tentativa = 0; tentativa < 20; tentativa++) {
            assertThat(tentar(filtro, "/api/busca/produtos", "200.100.50.1")).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("desligado, deixa tudo passar")
    void desligadoDeixaPassar() throws Exception {
        var filtro = filtroCom(false, 1);

        for (int tentativa = 0; tentativa < 30; tentativa++) {
            assertThat(tentar(filtro, LOGIN, "200.100.50.1")).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("usa o endereço real de tras do proxy da hospedagem")
    void usaEnderecoDeTrasDoProxy() throws Exception {
        var filtro = filtroCom(true, 1);

        var primeira = new MockHttpServletRequest("POST", LOGIN);
        primeira.setRemoteAddr("10.0.0.1");
        primeira.addHeader("X-Forwarded-For", "177.20.30.40, 10.0.0.1");
        var respostaUm = new MockHttpServletResponse();
        filtro.doFilter(primeira, respostaUm, mock(FilterChain.class));

        var segunda = new MockHttpServletRequest("POST", LOGIN);
        segunda.setRemoteAddr("10.0.0.1");
        segunda.addHeader("X-Forwarded-For", "177.20.30.99, 10.0.0.1");
        var respostaDois = new MockHttpServletResponse();
        filtro.doFilter(segunda, respostaDois, mock(FilterChain.class));

        assertThat(respostaUm.getStatus()).isEqualTo(200);
        assertThat(respostaDois.getStatus())
                .as("dois clientes diferentes atrás do mesmo proxy não dividem o balde")
                .isEqualTo(200);
    }

    @Test
    @DisplayName("a resposta de recusa e em portugues e diz quando tentar de novo")
    void respostaDeRecusa() throws Exception {
        var filtro = filtroCom(true, 1);
        tentar(filtro, LOGIN, "200.100.50.7");

        var requisicao = new MockHttpServletRequest("POST", LOGIN);
        requisicao.setRemoteAddr("200.100.50.7");
        var resposta = new MockHttpServletResponse();
        resposta.setCharacterEncoding("UTF-8");
        filtro.doFilter(requisicao, resposta, mock(FilterChain.class));

        assertThat(resposta.getStatus()).isEqualTo(429);
        assertThat(resposta.getHeader("Retry-After")).isEqualTo("60");
        assertThat(resposta.getContentAsString()).contains("Aguarde um minuto");
    }

    @Test
    @DisplayName("regra sem capacidade não e aceita na configuração")
    void recusaRegraSemCapacidade() {
        assertThatThrownBy(() -> new PropriedadesDeLimite.Regra("/api/**", 0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropriedadesDeLimite.Regra("/api/**", 5, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("sem regra configurada, o filtro não atrapalha")
    void semRegras() throws Exception {
        var filtro = new FiltroDeLimite(new PropriedadesDeLimite(true, null), new SimpleMeterRegistry());

        assertThat(tentar(filtro, LOGIN, "200.100.50.1")).isEqualTo(200);
    }
}
