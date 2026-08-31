package br.com.vitrinebauru.plataforma.tempo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * O relogio e injetado, nunca chamado direto por {@code Instant.now()}.
 *
 * <p>Sem isso, testar "cadastro parado ha mais de 15 dias na fila da SEDECON"
 * ou "token expirado" exigiria dormir de verdade no teste. Com o relogio como
 * dependencia, o teste avanca dias em uma linha.
 *
 * <p>UTC no servidor. O fuso de Bauru entra na hora de mostrar, no frontend, e
 * nao no armazenamento: guardar horario local e o caminho conhecido para o bug
 * que aparece uma vez por ano, na virada do horario de verao.
 */
@Configuration
public class ConfiguracaoDoRelogio {

    @Bean
    @ConditionalOnMissingBean
    public Clock relogio() {
        return Clock.systemUTC();
    }
}
