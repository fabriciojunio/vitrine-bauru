package br.com.vitrinebauru.unico;

import br.com.vitrinebauru.plataforma.seguranca.FiltroDeToken;
import br.com.vitrinebauru.plataforma.seguranca.PadraoDeSeguranca;
import br.com.vitrinebauru.plataforma.seguranca.PropriedadesDeSeguranca;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * A cadeia de segurança do processo único: uma só, cobrindo os quatro
 * serviços.
 *
 * <p>É a soma exata das quatro cadeias que existem quando os serviços rodam
 * separados, e ela precisa continuar sendo. Toda rota pública listada aqui tem
 * gêmea na configuração do serviço correspondente; quem acrescentar uma rota
 * lá e esquecer daqui vai descobrir na demonstração, que é o pior lugar para
 * descobrir. Por isso existe um teste que percorre as duas listas.
 */
@Configuration
@EnableMethodSecurity
public class SegurancaUnica {

    @Bean
    public PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain cadeiaUnica(HttpSecurity http,
                                           PropriedadesDeSeguranca propriedades,
                                           FiltroDeToken filtroDeToken,
                                           ObjectMapper mapeador) throws Exception {
        PadraoDeSeguranca.aplicar(http, propriedades, filtroDeToken, mapeador);

        http.authorizeHttpRequests(rotas -> rotas
                // Cadastro: criar conta e entrar são públicos.
                .requestMatchers(HttpMethod.POST, "/api/cadastro/empreendedores").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/cadastro/auth/login",
                        "/api/cadastro/auth/renovar", "/api/cadastro/auth/sair",
                        "/api/cadastro/auth/demonstracao").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/cadastro/auth/demonstracao",
                        "/api/cadastro/bairros", "/api/cadastro/categorias").permitAll()

                // Catálogo: categorias e imagens aparecem na vitrine pública.
                .requestMatchers(HttpMethod.GET, "/api/catalogo/categorias",
                        "/api/catalogo/imagens/**").permitAll()

                // Busca: a vitrine inteira é pública, inclusive o registro de
                // contato, que é disparado por quem não tem conta.
                .requestMatchers("/api/busca/**").permitAll()

                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                .requestMatchers("/actuator/**").hasRole("ADMIN_SEDECON")
                .requestMatchers("/api/cadastro/moderacao/**").hasRole("ADMIN_SEDECON")
                .requestMatchers("/api/notificacoes/**").hasRole("ADMIN_SEDECON")

                .anyRequest().authenticated());

        return http.build();
    }
}
