package br.com.vitrinebauru.notificacoes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Serviço de notificações: e-mail transacional disparado por evento. */
@SpringBootApplication(scanBasePackages = {
        "br.com.vitrinebauru.notificacoes",
        "br.com.vitrinebauru.plataforma"})
@EnableJpaRepositories(basePackages = {
        "br.com.vitrinebauru.notificacoes",
        "br.com.vitrinebauru.plataforma"})
@EntityScan(basePackages = {
        "br.com.vitrinebauru.notificacoes",
        "br.com.vitrinebauru.plataforma"})
@ConfigurationPropertiesScan(basePackages = "br.com.vitrinebauru.notificacoes")
public class AplicacaoDeNotificacoes {

    public static void main(String[] argumentos) {
        SpringApplication.run(AplicacaoDeNotificacoes.class, argumentos);
    }
}
