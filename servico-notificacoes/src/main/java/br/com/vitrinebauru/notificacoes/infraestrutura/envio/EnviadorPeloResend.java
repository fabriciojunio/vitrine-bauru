package br.com.vitrinebauru.notificacoes.infraestrutura.envio;

import br.com.vitrinebauru.notificacoes.dominio.Notificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Envio de verdade, pelo Resend.
 *
 * <p>So entra em cena quando existe chave configurada. Sem chave, o servico
 * sobe com o registrador em log, o que permite rodar a plataforma inteira em
 * desenvolvimento e na demonstracao sem contratar nada nem vazar e-mail de
 * ninguem.
 *
 * <p>A camada gratuita do Resend cobre 3 mil e-mails por mes e 100 por dia,
 * que e mais do que o volume esperado: cada empreendedor recebe entre dois e
 * quatro e-mails na vida inteira do cadastro.
 */
@Component
@ConditionalOnProperty(name = "vitrine.email.ativo", havingValue = "true")
public class EnviadorPeloResend implements EnviadorDeEmail {

    private static final Logger log = LoggerFactory.getLogger(EnviadorPeloResend.class);

    private final RestClient cliente;
    private final String remetente;

    public EnviadorPeloResend(RestClient.Builder construtor,
                              @Value("${vitrine.email.chave}") String chave,
                              @Value("${vitrine.email.remetente}") String remetente,
                              @Value("${vitrine.email.url:https://api.resend.com}") String url) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        fabrica.setReadTimeout((int) Duration.ofSeconds(10).toMillis());

        this.cliente = construtor.clone()
                .baseUrl(url)
                .requestFactory(fabrica)
                .defaultHeader("Authorization", "Bearer " + chave)
                .build();
        this.remetente = remetente;

        log.info("Envio de e-mail pelo Resend, remetente {}", remetente);
    }

    @Override
    public void enviar(Notificacao notificacao) {
        cliente.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "from", remetente,
                        "to", notificacao.destinatario(),
                        "subject", notificacao.assunto(),
                        "text", notificacao.corpo()))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public String descricao() {
        return "resend";
    }
}
