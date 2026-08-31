package br.com.vitrinebauru.catalogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Serviço de catálogo: produtos, categorias e imagens. */
@SpringBootApplication(scanBasePackages = {
        "br.com.vitrinebauru.catalogo",
        "br.com.vitrinebauru.plataforma"})
@EnableJpaRepositories(basePackages = {
        "br.com.vitrinebauru.catalogo",
        "br.com.vitrinebauru.plataforma"})
@EntityScan(basePackages = {
        "br.com.vitrinebauru.catalogo",
        "br.com.vitrinebauru.plataforma"})
@ConfigurationPropertiesScan(basePackages = "br.com.vitrinebauru.catalogo")
public class AplicacaoDeCatalogo {

    public static void main(String[] argumentos) {
        SpringApplication.run(AplicacaoDeCatalogo.class, argumentos);
    }
}
