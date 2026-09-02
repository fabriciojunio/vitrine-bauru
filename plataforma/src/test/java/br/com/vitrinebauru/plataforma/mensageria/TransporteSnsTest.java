package br.com.vitrinebauru.plataforma.mensageria;

import br.com.vitrinebauru.plataforma.observabilidade.RastroDaMensagem;
import br.com.vitrinebauru.plataforma.observabilidade.RastroDeTeste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O que o publicador manda para o SNS.
 *
 * <p>Este teste existe por uma razão específica: os dois atributos que ele
 * confere são invisíveis em teste manual. Publicar sem eles funciona, o
 * console mostra a mensagem entregue, e o consumidor do outro lado é que
 * descobre que não sabe de qual tópico a mensagem veio.
 */
@DisplayName("Transporte SNS")
@ExtendWith(MockitoExtension.class)
class TransporteSnsTest {

    private static final String ARN = "arn:aws:sns:sa-east-1:000000000000:vitrine-empreendedores";

    @Mock
    private SnsClient sns;

    private TransporteSns transporte;

    private final RastroDeTeste.Montagem montagem = RastroDeTeste.montar();

    private TransporteSns comArn() {
        return new TransporteSns(sns, new ArnDosTopicos(Map.of("vitrine.empreendedores", ARN)),
                montagem.rastro());
    }

    @Test
    @DisplayName("publica no ARN do tópico, com a carga intacta")
    void publicaNoArn() {
        transporte = comArn();

        transporte.enviar("vitrine.empreendedores", "1234", "{\"tipo\":\"CadastroAprovado\"}");

        PublishRequest pedido = capturar();
        assertThat(pedido.topicArn()).isEqualTo(ARN);
        assertThat(pedido.message()).isEqualTo("{\"tipo\":\"CadastroAprovado\"}");
    }

    @Test
    @DisplayName("manda o tópico de origem como atributo, que é como o consumidor descobre de onde veio")
    void mandaOTopicoComoAtributo() {
        transporte = comArn();

        transporte.enviar("vitrine.empreendedores", "1234", "{}");

        assertThat(atributo(capturar(), TransporteSns.ATRIBUTO_TOPICO))
                .isEqualTo("vitrine.empreendedores");
    }

    @Test
    @DisplayName("manda a chave como atributo, para achar o dono de uma mensagem parada na fila morta")
    void mandaAChaveComoAtributo() {
        transporte = comArn();

        transporte.enviar("vitrine.empreendedores", "1234", "{}");

        assertThat(atributo(capturar(), TransporteSns.ATRIBUTO_CHAVE)).isEqualTo("1234");
    }

    @Test
    @DisplayName("chave nula vira texto vazio, porque o SNS recusa atributo sem valor")
    void chaveNulaNaoQuebra() {
        transporte = comArn();

        transporte.enviar("vitrine.empreendedores", null, "{}");

        assertThat(atributo(capturar(), TransporteSns.ATRIBUTO_CHAVE)).isEmpty();
    }

    @Test
    @DisplayName("falha do SNS sobe, para o outbox contar a tentativa em vez de dar a mensagem por publicada")
    void falhaSobe() {
        transporte = comArn();
        when(sns.publish(any(PublishRequest.class)))
                .thenThrow(InvalidParameterException.builder().message("sem permissão").build());

        assertThatThrownBy(() -> transporte.enviar("vitrine.empreendedores", "1", "{}"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("tópico que ninguém declarou falha na hora, e não com mensagem sumindo depois")
    void topicoDesconhecido() {
        transporte = comArn();

        assertThatThrownBy(() -> transporte.enviar("vitrine.inexistente", "1", "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vitrine.inexistente");
    }

    @Test
    @DisplayName("leva o rastro em atributo quando há rastro em andamento")
    void levaORastro() {
        transporte = comArn();
        var trecho = montagem.rastro().retomar(null, "outbox publicar");
        var contexto = new io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext();

        try (var ignorado = contexto.maybeScope(trecho.context())) {
            transporte.enviar("vitrine.empreendedores", "1", "{}");
        }
        trecho.end();

        assertThat(atributo(capturar(), RastroDaMensagem.CAMPO))
                .as("sem isto o consumidor do outro lado começa um rastro solto")
                .contains(trecho.context().traceId());
    }

    @Test
    @DisplayName("sem rastro em andamento, o atributo simplesmente não vai, e o SNS não recusa a mensagem")
    void semRastroNaoMandaOAtributo() {
        transporte = comArn();

        transporte.enviar("vitrine.empreendedores", "1", "{}");

        assertThat(capturar().messageAttributes()).doesNotContainKey(RastroDaMensagem.CAMPO);
    }

    @Test
    @DisplayName("a descrição identifica o transporte no log e na métrica")
    void descricao() {
        assertThat(comArn().descricao()).isEqualTo("sns");
    }

    private PublishRequest capturar() {
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(sns).publish(captor.capture());
        return captor.getValue();
    }

    private static String atributo(PublishRequest pedido, String nome) {
        return pedido.messageAttributes().get(nome).stringValue();
    }
}
