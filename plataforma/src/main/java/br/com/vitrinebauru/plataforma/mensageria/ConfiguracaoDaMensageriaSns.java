package br.com.vitrinebauru.plataforma.mensageria;

import br.com.vitrinebauru.contratos.Topicos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Clientes e tópicos do transporte gerenciado.
 *
 * <p>Só entra em cena quando o transporte é o SNS. Nas outras duas
 * implantações nada disto é criado e o serviço sobe sem nenhuma dependência
 * de AWS, do mesmo jeito que já acontece com o Kafka.
 */
@Configuration
@ConditionalOnProperty(name = "vitrine.mensageria.transporte", havingValue = "sns")
public class ConfiguracaoDaMensageriaSns {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracaoDaMensageriaSns.class);

    /** Todos os tópicos do sistema. Qualquer serviço pode publicar em qualquer um. */
    private static final List<String> TODOS = List.of(
            Topicos.EMPREENDEDORES,
            Topicos.CATALOGO,
            Topicos.CONTATOS,
            Topicos.PRIVACIDADE);

    @Bean
    @ConditionalOnMissingBean
    public SnsClient clienteSns(PropriedadesDaAws propriedades) {
        SnsClientBuilder construtor = SnsClient.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .region(Region.of(propriedades.regiao()))
                .credentialsProvider(propriedades.credenciais());
        propriedades.enderecoAlternativo().ifPresent(construtor::endpointOverride);
        return construtor.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SqsClient clienteSqs(PropriedadesDaAws propriedades) {
        SqsClientBuilder construtor = SqsClient.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .region(Region.of(propriedades.regiao()))
                .credentialsProvider(propriedades.credenciais());
        propriedades.enderecoAlternativo().ifPresent(construtor::endpointOverride);
        return construtor.build();
    }

    /**
     * Cria os tópicos na subida e guarda os ARNs.
     *
     * <p>Os quatro serviços fazem isto ao mesmo tempo quando sobem juntos, e
     * está tudo bem: criar tópico é idempotente e devolve o ARN do que já
     * existe. É o mesmo espírito dos beans de {@code NewTopic} do lado do
     * Kafka, que também deixam a aplicação declarar o que precisa em vez de
     * depender de alguém ter rodado um script antes.
     */
    @Bean
    public ArnDosTopicos arnDosTopicos(SnsClient sns) {
        Map<String, String> arns = new LinkedHashMap<>();
        for (String topico : TODOS) {
            String nome = NomesNaAws.doTopico(topico);
            String arn = sns.createTopic(CreateTopicRequest.builder().name(nome).build()).topicArn();
            arns.put(topico, arn);
        }
        log.info("Tópicos SNS prontos: {}", arns.keySet());
        return new ArnDosTopicos(arns);
    }

    @Bean
    @ConditionalOnMissingBean
    public PropriedadesDaAws propriedadesDaAws(
            @Value("${vitrine.aws.regiao:sa-east-1}") String regiao,
            @Value("${vitrine.aws.endereco:}") String endereco,
            @Value("${vitrine.aws.chave:}") String chave,
            @Value("${vitrine.aws.segredo:}") String segredo) {
        return new PropriedadesDaAws(regiao, endereco, chave, segredo);
    }

    /**
     * O que o cliente precisa saber para falar com a AWS, ou com um substituto.
     *
     * <p>O endereço em branco é o caso normal: o SDK monta a URL do serviço
     * sozinho a partir da região. Preencher serve para apontar o cliente para
     * um servidor local durante teste, sem que a aplicação saiba a diferença.
     *
     * <p>Chave e segredo em branco também são o caso normal, e o desejável: em
     * máquina da AWS a credencial vem do papel atribuído à instância, e ter
     * chave em variável de ambiente é o que se quer evitar. O preenchimento
     * manual existe para rodar fora da AWS.
     */
    public record PropriedadesDaAws(String regiao, String endereco, String chave, String segredo) {

        java.util.Optional<URI> enderecoAlternativo() {
            return endereco == null || endereco.isBlank()
                    ? java.util.Optional.empty()
                    : java.util.Optional.of(URI.create(endereco));
        }

        software.amazon.awssdk.auth.credentials.AwsCredentialsProvider credenciais() {
            if (chave == null || chave.isBlank()) {
                return DefaultCredentialsProvider.create();
            }
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(chave, segredo));
        }
    }
}
