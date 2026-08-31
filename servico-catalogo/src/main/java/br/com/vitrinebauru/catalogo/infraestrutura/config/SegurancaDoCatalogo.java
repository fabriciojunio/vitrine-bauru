package br.com.vitrinebauru.catalogo.infraestrutura.config;

import br.com.vitrinebauru.plataforma.seguranca.FiltroDeToken;
import br.com.vitrinebauru.plataforma.seguranca.PadraoDeSeguranca;
import br.com.vitrinebauru.plataforma.seguranca.PropriedadesDeSeguranca;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Duas coisas são públicas no catálogo: a lista de categorias e as imagens.
 * O resto exige o token emitido pelo cadastro.
 *
 * <p>Desligado quando os quatro serviços rodam num processo só: lá existe uma
 * cadeia de segurança única, cobrindo todos os caminhos de uma vez. Duas
 * cadeias no mesmo contexto sem separação por caminho fariam a primeira
 * responder por tudo, e a segunda nunca valeria.
 */
@Configuration
@ConditionalOnProperty(name = "vitrine.servidor-unico", havingValue = "false", matchIfMissing = true)
@EnableMethodSecurity
public class SegurancaDoCatalogo {

    @Bean
    public SecurityFilterChain cadeiaDoCatalogo(HttpSecurity http,
                                                PropriedadesDeSeguranca propriedades,
                                                FiltroDeToken filtroDeToken,
                                                ObjectMapper mapeador) throws Exception {
        PadraoDeSeguranca.aplicar(http, propriedades, filtroDeToken, mapeador);

        http.authorizeHttpRequests(rotas -> rotas
                .requestMatchers(HttpMethod.GET, "/api/catalogo/categorias",
                        "/api/catalogo/imagens/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN_SEDECON")
                .anyRequest().authenticated());

        return http.build();
    }
}
