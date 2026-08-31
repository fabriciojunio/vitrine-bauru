package br.com.vitrinebauru.cadastro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Serviço de cadastro: contas, perfil do empreendedor e moderação da SEDECON.
 *
 * <p>Os pacotes estão listados a mão porque o serviço é composto por dois: o
 * próprio e a plataforma compartilhada. Sem isso, o Spring varreria só o
 * pacote desta classe e não acharia o outbox, que é justamente a peça cuja
 * ausência não daria erro nenhum na subida, só eventos que nunca saem.
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
