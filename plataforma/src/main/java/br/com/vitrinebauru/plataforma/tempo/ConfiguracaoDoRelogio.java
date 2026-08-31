package br.com.vitrinebauru.plataforma.tempo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * O relógio é injetado, nunca chamado direto por {@code Instant.now()}.
 *
 * <p>Sem isso, testar "cadastro parado há mais de 15 dias na fila da SEDECON"
 * ou "token expirado" exigiria dormir de verdade no teste. Com o relógio como
 * dependência, o teste avança dias em uma linha.
 *
 * <p>UTC no servidor. O fuso de Bauru entra na hora de mostrar, no frontend, e
 * não no armazenamento: guardar horário local é o caminho conhecido para o bug
 * que aparece uma vez por ano, na virada do horário de verao.
 */
@Configuration
public class ConfiguracaoDoRelogio {

    @Bean
    @ConditionalOnMissingBean
    public Clock relogio() {
        return Clock.systemUTC();
    }
}
