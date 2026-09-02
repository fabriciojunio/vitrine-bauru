package br.com.vitrinebauru.plataforma.mensageria;

import br.com.vitrinebauru.plataforma.observabilidade.RastroDaMensagem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Transporte gerenciado: publica no SNS, que entrega nas filas assinantes.
 *
 * <p>É o terceiro adaptador, e entrou por uma correção de premissa. O
 * documento de decisão anterior dizia que não existia mensageria gerenciada
 * com camada gratuita permanente, e isso estava errado: a busca tinha sido
 * por Kafka gerenciado, não pelo problema. SNS e SQS estão na camada
 * permanentemente gratuita da AWS, na casa de um milhão de mensagens por mês,
 * sem prazo para acabar.
 *
 * <p>O envio é síncrono pelo mesmo motivo do {@link TransporteKafka}: quem
 * chama é o publicador do outbox, e ele só pode marcar a mensagem como
 * publicada depois da entrega confirmada. Falha volta como exceção, o outbox
 * conta a tentativa e tenta de novo.
 *
 * <p>Ver docs/adr/0007-transporte-sns.md, principalmente a parte de ordenação,
 * que é o que se perde aqui e não se perde no Kafka.
 */
@Component
@ConditionalOnProperty(name = "vitrine.mensageria.transporte", havingValue = "sns")
public class TransporteSns implements TransporteDeEventos {

    /**
     * O tópico de origem viaja num atributo, não no corpo.
     *
     * <p>Com entrega bruta ligada, o corpo que chega na fila é exatamente o
     * JSON do evento, igual ao que o Kafka entrega. Sem isso, o SNS embrulha a
     * mensagem num envelope próprio e o consumidor precisaria desembrulhar
     * antes de desserializar, o que faria o caminho do SQS diferir do caminho
     * do Kafka justamente onde eles deveriam ser iguais.
     */
    static final String ATRIBUTO_TOPICO = "topico";

    /**
     * A chave também viaja como atributo, apesar de a fila comum não usar.
     *
     * <p>Ela não particiona nada aqui. Vai junto porque é o que aparece no log
     * e na mensagem parada na fila morta quando alguém precisa descobrir de
     * qual empreendedor era o evento que ficou preso.
     */
    static final String ATRIBUTO_CHAVE = "chave";

    private final SnsClient sns;
    private final ArnDosTopicos arns;
    private final RastroDaMensagem rastro;

    public TransporteSns(SnsClient sns, ArnDosTopicos arns, RastroDaMensagem rastro) {
        this.sns = sns;
        this.arns = arns;
        this.rastro = rastro;
    }

    @Override
    public void enviar(String topico, String chave, String carga) {
        Map<String, MessageAttributeValue> atributos = new HashMap<>();
        atributos.put(ATRIBUTO_TOPICO, texto(topico));
        atributos.put(ATRIBUTO_CHAVE, texto(chave));

        // Mesmo papel do cabeçalho no Kafka. O SNS entrega atributo de
        // mensagem para a fila junto com o corpo, então o rastro chega no
        // consumidor sem entrar no contrato do evento.
        String traceparent = rastro.capturar();
        if (traceparent != null) {
            atributos.put(RastroDaMensagem.CAMPO, texto(traceparent));
        }

        sns.publish(PublishRequest.builder()
                .topicArn(arns.de(topico))
                .message(carga)
                .messageAttributes(atributos)
                .build());
    }

    private static MessageAttributeValue texto(String valor) {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(valor == null ? "" : valor)
                .build();
    }

    @Override
    public String descricao() {
        return "sns";
    }
}
