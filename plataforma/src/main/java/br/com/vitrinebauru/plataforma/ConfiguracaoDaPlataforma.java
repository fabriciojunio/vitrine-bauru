package br.com.vitrinebauru.plataforma;

import br.com.vitrinebauru.plataforma.seguranca.PropriedadesDeSeguranca;
import br.com.vitrinebauru.plataforma.web.PropriedadesDeLimite;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * O que a plataforma liga sozinha em qualquer servico que a inclua.
 *
 * <p>O agendamento entra aqui porque o outbox depende dele: um servico que
 * esquecesse de habilitar tarefas agendadas gravaria evento no banco e nunca
 * publicaria, sem erro nenhum aparecer. Esse tipo de falha silenciosa e a que
 * custa mais caro para descobrir, entao ela nao pode depender de cada servico
 * lembrar de uma anotacao.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({PropriedadesDeSeguranca.class, PropriedadesDeLimite.class})
public class ConfiguracaoDaPlataforma {
}
