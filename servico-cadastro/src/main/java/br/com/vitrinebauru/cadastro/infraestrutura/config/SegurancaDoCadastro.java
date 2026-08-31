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
 * Quem entra onde no servico de cadastro.
 *
 * <p>A lista e curta e explicita. Tudo que nao esta escrito como publico exige
 * login, porque o padrao seguro precisa ser o silencio: esquecer de proteger
 * um endereco novo nao pode deixa-lo aberto.
 *
 * <p>Desligado quando os quatro servicos rodam num processo so: la existe uma
 * cadeia de seguranca unica, cobrindo todos os caminhos de uma vez. Duas
 * cadeias no mesmo contexto sem separacao por caminho fariam a primeira
 * responder por tudo, e a segunda nunca valeria.
 */
@Configuration
@ConditionalOnProperty(name = "vitrine.servidor-unico", havingValue = "false", matchIfMissing = true)
@EnableMethodSecurity
public class SegurancaDoCadastro {

    /**
     * Custo 12 no bcrypt: cerca de 250ms por verificacao em maquina modesta.
     * Lento o bastante para forca bruta nao compensar, rapido o bastante para
     * o login nao parecer travado. O custo fica gravado no proprio hash, entao
     * subir esse numero no futuro nao invalida as senhas ja cadastradas.
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
                // Metrica descreve o funcionamento interno e ajuda quem quer
                // atacar a escolher a hora. Fica atras de login.
                .requestMatchers("/actuator/**").hasRole("ADMIN_SEDECON")
                .requestMatchers("/api/cadastro/moderacao/**").hasRole("ADMIN_SEDECON")
                .anyRequest().authenticated());

        return http.build();
    }
}
