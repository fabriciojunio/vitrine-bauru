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
 * O servico da vitrine e publico por inteiro.
 *
 * <p>O que protege aqui nao e login, e limite de requisicao: o endereco esta
 * aberto na internet e um robo consegue varrer a base inteira. Como a base e
 * justamente a informacao que a plataforma existe para divulgar, a resposta
 * certa e limitar ritmo, e nao exigir cadastro do consumidor.
 *
 * <p>O actuator fica de fora dessa regra: metrica e detalhe de saude nao sao
 * conteudo de vitrine.
 *
 * <p>Desligado quando os quatro servicos rodam num processo so: la existe uma
 * cadeia de seguranca unica, cobrindo todos os caminhos de uma vez. Duas
 * cadeias no mesmo contexto sem separacao por caminho fariam a primeira
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
