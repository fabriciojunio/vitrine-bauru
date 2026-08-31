package br.com.vitrinebauru.unico;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Os quatro serviços num processo só.
 *
 * <h2>Por que isto existe</h2>
 * O projeto é de graduação e precisa ficar no ar sem custo. Quatro processos
 * Java, cada um com sua conexão de banco, mais um broker gerenciado, não cabem
 * em camada gratuita nenhuma: em 2026 não existe Kafka gerenciado com camada
 * gratuita permanente, e cada serviço separado no Render consumiria seu
 * próprio tempo de máquina e sofreria a própria partida a frio.
 *
 * <h2>O que muda e o que não muda</h2>
 * Muda o transporte: sem broker, o evento vai do outbox direto ao despachante,
 * dentro do mesmo processo. Muda a fronteira de processo, e com ela a
 * possibilidade de escalar um serviço sozinho.
 *
 * <p>Não muda nada do desenho: os módulos continuam separados, cada um com seu
 * esquema no banco, ninguém lê a tabela do vizinho, o evento continua passando
 * pelo outbox na mesma transação do estado e o consumidor continua idempotente
 * pelo inbox. É a mesma aplicação, empacotada de outro jeito.
 *
 * <p>Separar de volta em quatro processos é trocar uma variável de ambiente:
 * {@code TRANSPORTE_DE_EVENTOS=kafka} e subir cada serviço com o próprio
 * banco, que é exatamente o que o docker-compose e os manifestos do Kubernetes
 * fazem. Ver docs/adr/0002-transporte-de-eventos.md.
 */
// As tres anotacoes que o @SpringBootApplication junta, escritas a mao. O
// atalho nao serve aqui porque ele nao aceita filtro de varredura, e este
// modulo precisa de um: cada servico tem a propria classe de aplicacao, e sem
// exclui-las a varredura traria a configuracao automatica quatro vezes.
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {
                "br.com.vitrinebauru.plataforma",
                "br.com.vitrinebauru.cadastro",
                "br.com.vitrinebauru.catalogo",
                "br.com.vitrinebauru.busca",
                "br.com.vitrinebauru.notificacoes",
                "br.com.vitrinebauru.unico"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = SpringBootApplication.class))
@EnableJpaRepositories(basePackages = {
        "br.com.vitrinebauru.plataforma",
        "br.com.vitrinebauru.cadastro",
        "br.com.vitrinebauru.catalogo",
        "br.com.vitrinebauru.busca",
        "br.com.vitrinebauru.notificacoes"})
@EntityScan(basePackages = {
        "br.com.vitrinebauru.plataforma",
        "br.com.vitrinebauru.cadastro",
        "br.com.vitrinebauru.catalogo",
        "br.com.vitrinebauru.busca",
        "br.com.vitrinebauru.notificacoes"})
@ConfigurationPropertiesScan(basePackages = {
        "br.com.vitrinebauru.cadastro",
        "br.com.vitrinebauru.catalogo",
        "br.com.vitrinebauru.busca",
        "br.com.vitrinebauru.notificacoes"})
public class AplicacaoUnica {

    public static void main(String[] argumentos) {
        SpringApplication.run(AplicacaoUnica.class, argumentos);
    }
}
