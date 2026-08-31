package br.com.vitrinebauru.busca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Serviço de busca: a vitrine pública, alimentada por eventos. */
@SpringBootApplication(scanBasePackages = {
        "br.com.vitrinebauru.busca",
        "br.com.vitrinebauru.plataforma"})
@EnableJpaRepositories(basePackages = {
        "br.com.vitrinebauru.busca",
        "br.com.vitrinebauru.plataforma"})
@EntityScan(basePackages = {
        "br.com.vitrinebauru.busca",
        "br.com.vitrinebauru.plataforma"})
@ConfigurationPropertiesScan(basePackages = "br.com.vitrinebauru.busca")
public class AplicacaoDeBusca {

    public static void main(String[] argumentos) {
        SpringApplication.run(AplicacaoDeBusca.class, argumentos);
    }
}
