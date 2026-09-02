package br.com.vitrinebauru.plataforma.mensageria;

import br.com.vitrinebauru.plataforma.observabilidade.RastroDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Quando a mensagem é apagada da fila, e principalmente quando não é.
 *
 * <p>No SQS, quem apaga a mensagem é o consumidor, depois de processar. Errar
 * a ordem não quebra nada visivelmente: o sistema funciona, as filas ficam
 * limpas, e evento some quando um consumidor falha. Este teste é o que segura
 * essa ordem.
 */
@DisplayName("Ouvinte do SQS")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OuvinteDoSqsTest {

    private static final String FILA = "http://localhost:4566/000000000000/vitrine-busca";
    private static final String RECIBO = "recibo-1";

    @Mock
    private SqsClient sqs;

    @Mock
    private SnsClient sns;

    @Mock
    private Despachante despachante;

    private OuvinteDoSqs ouvinte;

    @BeforeEach
    void montar() {
        ouvinte = new OuvinteDoSqs(sqs, sns, despachante,
                new ArnDosTopicos(Map.of("vitrine.catalogo", "arn:aws:sns:sa-east-1:0:vitrine-catalogo")),
                RastroDeTeste.semColetor(),
                "busca");
        ouvinte.definirUrlDaFilaParaTeste(FILA);
    }

    @Test
    @DisplayName("entrega ao despachante e só então apaga da fila")
    void entregaEApaga() {
        chegam(mensagem(RECIBO, "vitrine.catalogo", "{\"tipo\":\"ProdutoPublicado\"}"));

        assertThat(ouvinte.processarUmLote()).isEqualTo(1);

        verify(despachante).despachar("vitrine.catalogo", "{\"tipo\":\"ProdutoPublicado\"}");
        verify(sqs).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("consumo que falha não apaga a mensagem, que volta quando a invisibilidade acaba")
    void falhaNaoApaga() {
        chegam(mensagem(RECIBO, "vitrine.catalogo", "{}"));
        doThrow(new IllegalStateException("banco fora"))
                .when(despachante).despachar(any(), any());

        assertThat(ouvinte.processarUmLote()).isZero();

        verify(sqs, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("uma mensagem que falha não impede as outras do mesmo lote")
    void umaFalhaNaoDerrubaOLote() {
        chegam(mensagem("recibo-a", "vitrine.catalogo", "{\"n\":1}"),
                mensagem("recibo-b", "vitrine.catalogo", "{\"n\":2}"));
        doThrow(new IllegalStateException("falhou só nesta"))
                .when(despachante).despachar("vitrine.catalogo", "{\"n\":1}");

        assertThat(ouvinte.processarUmLote()).isEqualTo(1);

        ArgumentCaptor<DeleteMessageRequest> apagadas =
                ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqs, times(1)).deleteMessage(apagadas.capture());
        assertThat(apagadas.getValue().receiptHandle()).isEqualTo("recibo-b");
    }

    @Test
    @DisplayName("mensagem sem o atributo de tópico não é despachada nem apagada")
    void semAtributoDeTopico() {
        Message estranha = Message.builder().receiptHandle(RECIBO).body("{}").build();
        chegam(estranha);

        assertThat(ouvinte.processarUmLote()).isZero();

        verify(despachante, never()).despachar(any(), any());
        verify(sqs, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("lê com espera longa, porque leitura vazia em laço queima a cota gratuita")
    void leituraComEsperaLonga() {
        chegam();

        ouvinte.processarUmLote();

        ArgumentCaptor<ReceiveMessageRequest> pedido =
                ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqs).receiveMessage(pedido.capture());
        assertThat(pedido.getValue().waitTimeSeconds()).isPositive();
        assertThat(pedido.getValue().queueUrl()).isEqualTo(FILA);
    }

    @Test
    @DisplayName("pede os dois atributos do publicador, senão o tópico chega nulo")
    void pedeOsAtributos() {
        chegam();

        ouvinte.processarUmLote();

        ArgumentCaptor<ReceiveMessageRequest> pedido =
                ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqs).receiveMessage(pedido.capture());
        assertThat(pedido.getValue().messageAttributeNames())
                .contains(TransporteSns.ATRIBUTO_TOPICO, TransporteSns.ATRIBUTO_CHAVE);
    }

    private void chegam(Message... mensagens) {
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(mensagens).build());
    }

    private static Message mensagem(String recibo, String topico, String corpo) {
        return Message.builder()
                .receiptHandle(recibo)
                .body(corpo)
                .messageAttributes(Map.of(TransporteSns.ATRIBUTO_TOPICO,
                        MessageAttributeValue.builder().dataType("String").stringValue(topico).build()))
                .build();
    }
}
