package br.com.vitrinebauru.cadastro.infraestrutura.externo;

import br.com.vitrinebauru.contratos.tipos.Documento;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

/**
 * Consulta a situação de um CNPJ na BrasilAPI.
 *
 * <p>A resposta é informativa, nunca decisória. Três razões: a BrasilAPI é um
 * projeto comunitário sem compromisso de disponibilidade, o cadastro aqui
 * aceita CPF de quem ainda não se formalizou, e reprovar automaticamente um
 * empreendedor de verdade por causa de uma API fora do ar seria o pior erro
 * que este sistema pode cometer.
 *
 * <p>Por isso: tempo de espera curto, disjuntor no caminho, e falha vira
 * {@code Optional.empty()}. Quem modera vê "não foi possível conferir agora" e
 * decide como sempre decidiu, olhando os dados.
 */
@Component
public class ConsultaDeDocumento {

    private static final Logger log = LoggerFactory.getLogger(ConsultaDeDocumento.class);

    private final RestClient cliente;
    private final boolean ativa;

    public ConsultaDeDocumento(RestClient.Builder construtor,
                               @Value("${vitrine.brasilapi.url:https://brasilapi.com.br/api}") String url,
                               @Value("${vitrine.brasilapi.ativa:true}") boolean ativa) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        fabrica.setReadTimeout((int) Duration.ofSeconds(5).toMillis());

        this.cliente = construtor.clone()
                .baseUrl(url)
                .requestFactory(fabrica)
                .build();
        this.ativa = ativa;
    }

    @CircuitBreaker(name = "brasilapi", fallbackMethod = "naoDeuParaConferir")
    public Optional<Situacao> consultar(Documento documento) {
        if (!ativa || !documento.ehCnpj()) {
            return Optional.empty();
        }

        Resposta resposta = cliente.get()
                .uri("/cnpj/v1/{cnpj}", documento.valor())
                .retrieve()
                .body(Resposta.class);

        if (resposta == null) {
            return Optional.empty();
        }

        return Optional.of(new Situacao(
                resposta.razao_social(),
                resposta.nome_fantasia(),
                resposta.descricao_situacao_cadastral(),
                resposta.municipio(),
                "ATIVA".equalsIgnoreCase(resposta.descricao_situacao_cadastral())));
    }

    @SuppressWarnings("unused")
    private Optional<Situacao> naoDeuParaConferir(Documento documento, Throwable erro) {
        log.warn("Consulta de CNPJ indisponivel ({}). O cadastro segue para analise manual.",
                erro.getMessage());
        return Optional.empty();
    }

    public record Situacao(String razaoSocial, String nomeFantasia, String situacaoCadastral,
                           String municipio, boolean ativa) {
    }

    /**
     * Espelha o JSON da BrasilAPI, inclusive o nome dos campos com sublinhado.
     * Renomear aqui exigiria anotação em cada campo e não melhoraria nada:
     * este record não sai desta classe.
     */
    private record Resposta(String razao_social, String nome_fantasia,
                            String descricao_situacao_cadastral, String municipio) {
    }
}
