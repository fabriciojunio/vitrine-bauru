package br.com.vitrinebauru.plataforma.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Segura força bruta no login e abuso nos endpoints públicos.
 *
 * <p>O balde vive na memória do processo. Com várias instâncias, cada uma tem
 * o próprio balde, e o limite efetivo multiplica pelo número de instâncias.
 * Isso é aceitável aqui e a alternativa não seria: um contador compartilhado
 * exigiria Redis, que custa dinheiro e ainda vira ponto único de falha num
 * projeto que precisa caber em camada gratuita. Contra ataque de força bruta,
 * limite aproximado resolve; contra tentativa distribuída de verdade, quem
 * resolve é a senha com bcrypt e o bloqueio de conta, que existem no cadastro.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FiltroDeLimite extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroDeLimite.class);
    private static final long MINUTOS_SEM_USO_PARA_DESCARTAR = 30;

    private final PropriedadesDeLimite propriedades;
    private final MeterRegistry metricas;
    private final AntPathMatcher comparador = new AntPathMatcher();
    private final Map<String, BaldeUsado> baldes = new ConcurrentHashMap<>();

    public FiltroDeLimite(PropriedadesDeLimite propriedades, MeterRegistry metricas) {
        this.propriedades = propriedades;
        this.metricas = metricas;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain corrente) throws ServletException, IOException {
        Optional<PropriedadesDeLimite.Regra> regra = regraDe(requisicao);

        if (!propriedades.ativo() || regra.isEmpty()) {
            corrente.doFilter(requisicao, resposta);
            return;
        }

        String chave = regra.get().padrao() + "|" + origem(requisicao);
        BaldeUsado balde = baldes.computeIfAbsent(chave, ignorado -> new BaldeUsado(criar(regra.get())));

        if (balde.tentarConsumir()) {
            corrente.doFilter(requisicao, resposta);
            return;
        }

        metricas.counter("vitrine.limite.bloqueios", "padrao", regra.get().padrao()).increment();
        log.warn("Limite atingido em {} pela origem {}", requisicao.getRequestURI(), origem(requisicao));
        recusar(resposta, regra.get());
    }

    private Optional<PropriedadesDeLimite.Regra> regraDe(HttpServletRequest requisicao) {
        String caminho = requisicao.getRequestURI();
        return propriedades.regras().stream()
                .filter(regra -> comparador.match(regra.padrao(), caminho))
                .findFirst();
    }

    private Bucket criar(PropriedadesDeLimite.Regra regra) {
        Bandwidth limite = Bandwidth.builder()
                .capacity(regra.capacidade())
                .refillGreedy(regra.capacidade(), regra.janela())
                .build();
        return Bucket.builder().addLimit(limite).build();
    }

    /**
     * Atrás do proxy da hospedagem, {@code getRemoteAddr} devolve o IP do
     * próprio proxy e todo mundo compartilharia o mesmo balde. O primeiro
     * endereço do X-Forwarded-For é o do cliente.
     */
    private String origem(HttpServletRequest requisicao) {
        String encaminhado = requisicao.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return requisicao.getRemoteAddr();
    }

    private void recusar(HttpServletResponse resposta, PropriedadesDeLimite.Regra regra) throws IOException {
        resposta.setStatus(429);
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        resposta.setHeader("Retry-After", String.valueOf(regra.janela().toSeconds()));
        resposta.getWriter().write("""
                {"status":429,"title":"Muitas tentativas",\
                "detail":"Você tentou vezes demais em pouco tempo. Aguarde um minuto e tente de novo."}""");
    }

    /** Balde de quem parou de aparecer não precisa ocupar memória para sempre. */
    @Scheduled(fixedDelay = 600_000)
    public void descartarBaldesParados() {
        Instant limite = Instant.now().minusSeconds(MINUTOS_SEM_USO_PARA_DESCARTAR * 60);
        baldes.entrySet().removeIf(entrada -> entrada.getValue().ultimoUso.isBefore(limite));
    }

    private static final class BaldeUsado {
        private final Bucket balde;
        private volatile Instant ultimoUso;

        private BaldeUsado(Bucket balde) {
            this.balde = balde;
            this.ultimoUso = Instant.now();
        }

        private boolean tentarConsumir() {
            ultimoUso = Instant.now();
            return balde.tryConsume(1);
        }
    }
}
