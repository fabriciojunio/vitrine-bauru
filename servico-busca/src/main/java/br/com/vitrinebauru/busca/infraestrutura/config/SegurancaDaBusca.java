package br.com.vitrinebauru.busca.infraestrutura.config;

import br.com.vitrinebauru.plataforma.seguranca.FiltroDeToken;
import br.com.vitrinebauru.plataforma.seguranca.PadraoDeSeguranca;
import br.com.vitrinebauru.plataforma.seguranca.PropriedadesDeSeguranca;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * O serviço da vitrine é público por inteiro.
 *
 * <p>O que protege aqui não é login, é limite de requisição: o endereço está
 * aberto na internet e um robô consegue varrer a base inteira. Como a base é
 * justamente a informação que a plataforma existe para divulgar, a resposta
 * certa é limitar ritmo, e não exigir cadastro do consumidor.
 *
 * <p>O actuator fica de fora dessa regra: métrica e detalhe de saúde não são
 * conteúdo de vitrine.
 *
 * <p>Desligado quando os quatro serviços rodam num processo só: lá existe uma
 * cadeia de segurança única, cobrindo todos os caminhos de uma vez. Duas
 * cadeias no mesmo contexto sem separação por caminho fariam a primeira
 * responder por tudo, e a segunda nunca valeria.
 */
@Configuration
@ConditionalOnProperty(name = "vitrine.servidor-unico", havingValue = "false", matchIfMissing = true)
public class SegurancaDaBusca {

    @Bean
    public SecurityFilterChain cadeiaDaBusca(HttpSecurity http,
                                             PropriedadesDeSeguranca propriedades,
                                             FiltroDeToken filtroDeToken,
                                             ObjectMapper mapeador) throws Exception {
        PadraoDeSeguranca.aplicar(http, propriedades, filtroDeToken, mapeador);

        http.authorizeHttpRequests(rotas -> rotas
                .requestMatchers("/api/busca/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN_SEDECON")
                .anyRequest().authenticated());

        return http.build();
    }
}
