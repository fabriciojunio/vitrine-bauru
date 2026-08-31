package br.com.vitrinebauru.plataforma.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.util.List;

/**
 * O que todo servico configura igual.
 *
 * <p>Existe para a regra de seguranca nao ser copiada e colada quatro vezes,
 * porque regra copiada e regra que fica desatualizada em tres dos quatro
 * lugares. Cada servico ainda declara suas proprias rotas publicas e
 * protegidas; o que vem daqui e o resto.
 */
public final class PadraoDeSeguranca {

    private PadraoDeSeguranca() {
    }

    public static void aplicar(HttpSecurity http, PropriedadesDeSeguranca propriedades,
                               FiltroDeToken filtroDeToken, ObjectMapper mapeador) throws Exception {
        http
                // Sem CSRF porque a autenticacao vai no cabecalho Authorization,
                // e nao em cookie. Token que o navegador nao anexa sozinho nao
                // e vulneravel a requisicao forjada de outro site. Se um dia a
                // sessao virar cookie, esta linha precisa voltar.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(origensPermitidas(propriedades)))
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(cabecalhos -> cabecalhos
                        .contentSecurityPolicy(politica ->
                                politica.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(referencia -> referencia.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                        .ReferrerPolicy.NO_REFERRER)))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint((requisicao, resposta, excecao) ->
                                responder(resposta, mapeador, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Não autenticado",
                                        "Faça login para continuar."))
                        .accessDeniedHandler((requisicao, resposta, excecao) ->
                                responder(resposta, mapeador, HttpServletResponse.SC_FORBIDDEN,
                                        "Sem permissão",
                                        "Sua conta não tem permissão para esta ação.")))
                .addFilterBefore(filtroDeToken, UsernamePasswordAuthenticationFilter.class);
    }

    private static UrlBasedCorsConfigurationSource origensPermitidas(PropriedadesDeSeguranca propriedades) {
        var configuracao = new CorsConfiguration();
        // Lista explicita, nunca "*". Em producao o valor vem do ambiente e
        // aponta so para o dominio do frontend.
        configuracao.setAllowedOrigins(propriedades.origensPermitidas());
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlacao"));
        configuracao.setExposedHeaders(List.of("X-Correlacao"));
        configuracao.setMaxAge(3600L);

        var fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", configuracao);
        return fonte;
    }

    private static void responder(HttpServletResponse resposta, ObjectMapper mapeador,
                                  int codigo, String titulo, String detalhe) throws java.io.IOException {
        var problema = ProblemDetail.forStatus(codigo);
        problema.setTitle(titulo);
        problema.setDetail(detalhe);
        problema.setType(URI.create("https://vitrinebauru.com.br/erros/" + codigo));

        resposta.setStatus(codigo);
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        mapeador.writeValue(resposta.getOutputStream(), problema);
    }
}
