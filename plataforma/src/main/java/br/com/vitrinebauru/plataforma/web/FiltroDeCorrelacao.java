package br.com.vitrinebauru.plataforma.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Carimba um identificador em toda requisição.
 *
 * <p>Com quatro serviços e mensagens assíncronas, "o cadastro do fulano não
 * apareceu na busca" é uma investigação em quatro logs diferentes. A
 * correlação atravessa a chamada HTTP, entra no evento gravado no outbox e
 * reaparece no log de quem consumiu, do outro lado. Sem ela, cruzar isso é
 * comparar horário a olho.
 *
 * <p>Vai primeiro na fila de filtros: se estourar erro antes disto, o log sai
 * sem identificador e o rastro se perde exatamente no caso em que mais
 * importa.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroDeCorrelacao extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Correlacao";
    public static final String CHAVE_NO_LOG = "correlacao";

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain corrente) throws ServletException, IOException {
        String recebida = requisicao.getHeader(CABECALHO);
        String correlacao = ehIdentificadorValido(recebida) ? recebida : UUID.randomUUID().toString();

        MDC.put(CHAVE_NO_LOG, correlacao);
        resposta.setHeader(CABECALHO, correlacao);
        try {
            corrente.doFilter(requisicao, resposta);
        } finally {
            MDC.remove(CHAVE_NO_LOG);
        }
    }

    /**
     * Aceita só UUID vindo de fora. O cabeçalho é escrito pelo cliente e cai
     * no log; aceitar texto livre deixaria alguém injetar quebra de linha e
     * forjar entrada de log.
     */
    private boolean ehIdentificadorValido(String valor) {
        if (valor == null) {
            return false;
        }
        try {
            UUID.fromString(valor);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
