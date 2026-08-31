package br.com.vitrinebauru.notificacoes.infraestrutura.config;

import br.com.vitrinebauru.plataforma.seguranca.FiltroDeToken;
import br.com.vitrinebauru.plataforma.seguranca.PadraoDeSeguranca;
import br.com.vitrinebauru.plataforma.seguranca.PropriedadesDeSeguranca;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Nada aqui e publico: o servico so escuta evento e guarda historico de
 * e-mail. O unico endereco exposto e a consulta da SEDECON.
 *
 * <p>Desligado quando os quatro servicos rodam num processo so: la existe uma
 * cadeia de seguranca unica, cobrindo todos os caminhos de uma vez. Duas
 * cadeias no mesmo contexto sem separacao por caminho fariam a primeira
 * responder por tudo, e a segunda nunca valeria.
 */
@Configuration
@ConditionalOnProperty(name = "vitrine.servidor-unico", havingValue = "false", matchIfMissing = true)
@EnableMethodSecurity
public class SegurancaDasNotificacoes {

    @Bean
    public SecurityFilterChain cadeiaDasNotificacoes(HttpSecurity http,
                                                PropriedadesDeSeguranca propriedades,
                                                FiltroDeToken filtroDeToken,
                                                ObjectMapper mapeador) throws Exception {
        PadraoDeSeguranca.aplicar(http, propriedades, filtroDeToken, mapeador);

        http.authorizeHttpRequests(rotas -> rotas
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN_SEDECON")
                .anyRequest().authenticated());

        return http.build();
    }
}
