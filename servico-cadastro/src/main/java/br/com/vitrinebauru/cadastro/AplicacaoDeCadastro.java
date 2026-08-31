package br.com.vitrinebauru.cadastro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Serviço de cadastro: contas, perfil do empreendedor e moderação da SEDECON.
 *
 * <p>Os pacotes estao listados a mao porque o servico e composto por dois: o
 * proprio e a plataforma compartilhada. Sem isso, o Spring varreria so o
 * pacote desta classe e nao acharia o outbox, que e justamente a peca cuja
 * ausencia nao daria erro nenhum na subida, so eventos que nunca saem.
 */
@SpringBootApplication(scanBasePackages = {
        "br.com.vitrinebauru.cadastro",
        "br.com.vitrinebauru.plataforma"})
@EnableJpaRepositories(basePackages = {
        "br.com.vitrinebauru.cadastro",
        "br.com.vitrinebauru.plataforma"})
@EntityScan(basePackages = {
        "br.com.vitrinebauru.cadastro",
        "br.com.vitrinebauru.plataforma"})
@ConfigurationPropertiesScan(basePackages = "br.com.vitrinebauru.cadastro")
public class AplicacaoDeCadastro {

    public static void main(String[] argumentos) {
        SpringApplication.run(AplicacaoDeCadastro.class, argumentos);
    }
}
