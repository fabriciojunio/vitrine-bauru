package br.com.vitrinebauru.plataforma.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Correlacao entre servicos")
class CorrelacaoTest {

    private final FiltroDeCorrelacao filtro = new FiltroDeCorrelacao();

    @AfterEach
    void limpar() {
        MDC.clear();
    }

    @Test
    @DisplayName("aproveita a correlacao que veio do cliente")
    void aproveitaAQueVeio() throws Exception {
        String vinda = UUID.randomUUID().toString();
        var requisicao = new MockHttpServletRequest();
        requisicao.addHeader(FiltroDeCorrelacao.CABECALHO, vinda);
        var resposta = new MockHttpServletResponse();

        filtro.doFilter(requisicao, resposta, mock(FilterChain.class));

        assertThat(resposta.getHeader(FiltroDeCorrelacao.CABECALHO)).isEqualTo(vinda);
    }

    @Test
    @DisplayName("cria uma correlacao quando o cliente nao mandou")
    void criaQuandoNaoVeio() throws Exception {
        var resposta = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest(), resposta, mock(FilterChain.class));

        assertThat(resposta.getHeader(FiltroDeCorrelacao.CABECALHO)).isNotNull();
        assertThat(UUID.fromString(resposta.getHeader(FiltroDeCorrelacao.CABECALHO))).isNotNull();
    }

    @Test
    @DisplayName("descarta texto livre no cabecalho, que serviria para forjar linha de log")
    void descartaTextoLivre() throws Exception {
        var requisicao = new MockHttpServletRequest();
        requisicao.addHeader(FiltroDeCorrelacao.CABECALHO, "linha1\nlinha2 usuario=admin");
        var resposta = new MockHttpServletResponse();

        filtro.doFilter(requisicao, resposta, mock(FilterChain.class));

        assertThat(resposta.getHeader(FiltroDeCorrelacao.CABECALHO)).doesNotContain("admin");
    }

    @Test
    @DisplayName("limpa o registro depois da requisicao, para a thread reaproveitada nao herdar")
    void limpaDepois() throws Exception {
        filtro.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(MDC.get(FiltroDeCorrelacao.CHAVE_NO_LOG)).isNull();
    }

    @Test
    @DisplayName("fora de requisicao, gera uma correlacao em vez de devolver nulo")
    void foraDeRequisicao() {
        assertThat(Correlacao.atual()).isNotNull();
    }

    @Test
    @DisplayName("dentro da requisicao, usa a correlacao do registro de log")
    void dentroDaRequisicao() {
        UUID esperada = UUID.randomUUID();
        MDC.put(FiltroDeCorrelacao.CHAVE_NO_LOG, esperada.toString());

        assertThat(Correlacao.atual()).isEqualTo(esperada);
    }

    @Test
    @DisplayName("registro corrompido nao derruba a criacao do evento")
    void registroCorrompido() {
        MDC.put(FiltroDeCorrelacao.CHAVE_NO_LOG, "nao-e-uuid");

        assertThat(Correlacao.atual()).isNotNull();
    }
}
