package br.com.vitrinebauru.cadastro.infraestrutura.config;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Quem entra onde no serviço de cadastro.
 *
 * <p>A lista é curta e explícita. Tudo que não está escrito como público exige
 * login, porque o padrão seguro precisa ser o silêncio: esquecer de proteger
 * um endereço novo não pode deixa-lo aberto.
 *
 * <p>Desligado quando os quatro serviços rodam num processo só: lá existe uma
 * cadeia de segurança única, cobrindo todos os caminhos de uma vez. Duas
 * cadeias no mesmo contexto sem separação por caminho fariam a primeira
 * responder por tudo, e a segunda nunca valeria.
 */
@Configuration
@ConditionalOnProperty(name = "vitrine.servidor-unico", havingValue = "false", matchIfMissing = true)
@EnableMethodSecurity
public class SegurancaDoCadastro {

    /**
     * Custo 12 no bcrypt: cerca de 250ms por verificação em máquina modesta.
     * Lento o bastante para força bruta não compensar, rápido o bastante para
     * o login não parecer travado. O custo fica gravado no próprio hash, então
     * subir esse número no futuro não inválida as senhas já cadastradas.
     */
    @Bean
    public PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain cadeiaDoCadastro(HttpSecurity http,
                                                PropriedadesDeSeguranca propriedades,
                                                FiltroDeToken filtroDeToken,
                                                ObjectMapper mapeador) throws Exception {
        PadraoDeSeguranca.aplicar(http, propriedades, filtroDeToken, mapeador);

        http.authorizeHttpRequests(rotas -> rotas
                .requestMatchers(HttpMethod.POST, "/api/cadastro/empreendedores").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/cadastro/auth/login",
                        "/api/cadastro/auth/renovar", "/api/cadastro/auth/sair",
                        "/api/cadastro/auth/demonstracao").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/cadastro/auth/demonstracao",
                        "/api/cadastro/bairros", "/api/cadastro/categorias").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Métrica descreve o funcionamento interno e ajuda quem quer
                // atacar a escolher a hora. Fica atrás de login.
                .requestMatchers("/actuator/**").hasRole("ADMIN_SEDECON")
                .requestMatchers("/api/cadastro/moderacao/**").hasRole("ADMIN_SEDECON")
                .anyRequest().authenticated());

        return http.build();
    }
}
