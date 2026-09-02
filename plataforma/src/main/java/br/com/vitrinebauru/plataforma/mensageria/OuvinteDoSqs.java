package br.com.vitrinebauru.plataforma.mensageria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Liga o consumo da fila SQS aos consumidores registrados no serviço.
 *
 * <p>É o par do {@link OuvinteDoKafka} para o transporte gerenciado, e segue o
 * mesmo desenho: a lista de tópicos vem do {@link Despachante} em tempo de
 * execução, e serviço sem consumidor nenhum não abre conexão de consumo.
 *
 * <h2>Por que a mensagem só é apagada no fim</h2>
 * No SQS, receber não apaga: a mensagem fica invisível por um tempo e volta
 * sozinha se ninguém apagar. É por isso que o {@code deleteMessage} está
 * depois do despacho e não antes. Apagar ao receber transformaria qualquer
 * falha de consumidor em evento perdido em silêncio, que é exatamente o que o
 * outbox e o inbox existem para impedir do outro lado.
 *
 * <p>Reentrega repetida não vira laço infinito porque a fila é criada com
 * política de redirecionamento: depois de algumas tentativas a mensagem vai
 * para a fila morta em vez de circular para sempre. É o mesmo papel do
 * tratador de erro com fila morta do lado do Kafka.
 */
@Component
@ConditionalOnProperty(name = "vitrine.mensageria.transporte", havingValue = "sns")
public class OuvinteDoSqs implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(OuvinteDoSqs.class);

    /**
     * Espera longa na leitura.
     *
     * <p>Com espera zero, o cliente pergunta e volta vazio o tempo todo, o que
     * queima a cota gratuita de requisição sem entregar mensagem nenhuma. Com
     * vinte segundos, a chamada fica aberta esperando, e o mesmo milhão de
     * requisições por mês passa a durar muito mais.
     */
    private static final int ESPERA_DA_LEITURA_S = 20;

    /** Teto do lote numa leitura do SQS. */
    private static final int MENSAGENS_POR_LEITURA = 10;

    /**
     * Tempo que a mensagem fica invisível depois de entregue.
     *
     * <p>Precisa ser maior que o pior caso de processamento, senão a mensagem
     * reaparece e é processada em paralelo com a tentativa que ainda está
     * rodando. O inbox impede o efeito duplicado, mas o trabalho é feito duas
     * vezes à toa.
     */
    private static final int INVISIBILIDADE_S = 60;

    /** Depois disto a mensagem vai para a fila morta, como no Kafka. */
    private static final int TENTATIVAS_ATE_A_FILA_MORTA = 3;

    private final SqsClient sqs;
    private final SnsClient sns;
    private final Despachante despachante;
    private final ArnDosTopicos arns;
    private final String grupo;

    private final AtomicBoolean rodando = new AtomicBoolean(false);
    private ExecutorService thread;
    private String urlDaFila;

    public OuvinteDoSqs(SqsClient sqs,
                        SnsClient sns,
                        Despachante despachante,
                        ArnDosTopicos arns,
                        @Value("${vitrine.mensageria.grupo}") String grupo) {
        this.sqs = sqs;
        this.sns = sns;
        this.despachante = despachante;
        this.arns = arns;
        this.grupo = grupo;
    }

    @Override
    public void start() {
        Set<String> topicos = despachante.topicosAssinados();
        if (topicos.isEmpty()) {
            log.info("Nenhum consumidor registrado: este serviço só publica");
            return;
        }

        urlDaFila = prepararFila();
        assinar(topicos);

        rodando.set(true);
        thread = Executors.newSingleThreadExecutor(tarefa -> {
            Thread t = new Thread(tarefa, "sqs-" + grupo);
            t.setDaemon(true);
            return t;
        });
        thread.submit(this::laco);
        log.info("Consumindo {} para os tópicos {}", NomesNaAws.daFila(grupo), topicos);
    }

    /**
     * Cria a fila morta, a fila principal apontando para ela, e libera o SNS
     * para escrever nessa fila.
     *
     * <p>A liberação não é detalhe: a fila nasce só com permissão para o dono,
     * e o SNS é outro serviço. Sem esta política a assinatura é criada, tudo
     * parece certo no console, e a mensagem publicada simplesmente não chega.
     */
    private String prepararFila() {
        String urlDaMorta = sqs.createQueue(CreateQueueRequest.builder()
                .queueName(NomesNaAws.daFilaMorta(grupo))
                .build()).queueUrl();

        String arnDaMorta = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(urlDaMorta)
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build())
                .attributes().get(QueueAttributeName.QUEUE_ARN);

        String url = sqs.createQueue(CreateQueueRequest.builder()
                .queueName(NomesNaAws.daFila(grupo))
                .attributes(Map.of(
                        QueueAttributeName.VISIBILITY_TIMEOUT, String.valueOf(INVISIBILIDADE_S),
                        QueueAttributeName.REDRIVE_POLICY, """
                                {"deadLetterTargetArn":"%s","maxReceiveCount":"%d"}"""
                                .formatted(arnDaMorta, TENTATIVAS_ATE_A_FILA_MORTA)))
                .build()).queueUrl();

        String arnDaFila = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(url)
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build())
                .attributes().get(QueueAttributeName.QUEUE_ARN);

        sqs.setQueueAttributes(SetQueueAttributesRequest.builder()
                .queueUrl(url)
                .attributes(Map.of(QueueAttributeName.POLICY, politicaParaOSns(arnDaFila)))
                .build());

        return url;
    }

    private String politicaParaOSns(String arnDaFila) {
        String contaEPrefixo = arnDaFila.substring(0, arnDaFila.lastIndexOf(':'))
                .replace(":sqs:", ":sns:");
        return """
                {"Version":"2012-10-17","Statement":[{
                  "Effect":"Allow",
                  "Principal":{"Service":"sns.amazonaws.com"},
                  "Action":"sqs:SendMessage",
                  "Resource":"%s",
                  "Condition":{"ArnLike":{"aws:SourceArn":"%s:*"}}
                }]}""".formatted(arnDaFila, contaEPrefixo);
    }

    /**
     * Assina a fila em cada tópico que este serviço consome.
     *
     * <p>{@code RawMessageDelivery} ligado é o que faz o corpo que chega ser o
     * JSON do evento, e não um envelope do SNS com o evento dentro de um
     * campo. Sem isso o caminho do SQS precisaria de uma desserialização a
     * mais que o caminho do Kafka não tem, e os dois deixariam de ser
     * intercambiáveis de verdade.
     */
    private void assinar(Set<String> topicos) {
        String arnDaFila = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(urlDaFila)
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build())
                .attributes().get(QueueAttributeName.QUEUE_ARN);

        for (String topico : topicos) {
            sns.subscribe(SubscribeRequest.builder()
                    .topicArn(arns.de(topico))
                    .protocol("sqs")
                    .endpoint(arnDaFila)
                    .attributes(Map.of("RawMessageDelivery", "true"))
                    .returnSubscriptionArn(true)
                    .build());
        }
    }

    private void laco() {
        while (rodando.get()) {
            try {
                processarUmLote();
            } catch (RuntimeException erro) {
                // Falha de rede ou credencial não pode matar a thread, senão o
                // serviço fica no ar, saudável no health check, e sem consumir
                // nada. Loga e tenta de novo na volta do laço.
                log.error("Falha lendo a fila {}", NomesNaAws.daFila(grupo), erro);
                dormirUmPouco();
            }
        }
    }

    /**
     * Lê um lote e entrega cada mensagem.
     *
     * <p>Visível para o teste de propósito: é aqui que mora a decisão que mais
     * importa nesta classe, e testar isso pela thread do laço seria testar
     * escalonamento em vez de comportamento.
     */
    int processarUmLote() {
        List<Message> mensagens = sqs.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(urlDaFila)
                .maxNumberOfMessages(MENSAGENS_POR_LEITURA)
                .waitTimeSeconds(ESPERA_DA_LEITURA_S)
                .visibilityTimeout(INVISIBILIDADE_S)
                .messageAttributeNames(TransporteSns.ATRIBUTO_TOPICO, TransporteSns.ATRIBUTO_CHAVE)
                .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
                .build()).messages();

        int entregues = 0;
        for (Message mensagem : mensagens) {
            if (entregar(mensagem)) {
                entregues++;
            }
        }
        return entregues;
    }

    private boolean entregar(Message mensagem) {
        String topico = atributo(mensagem, TransporteSns.ATRIBUTO_TOPICO);
        if (topico == null) {
            // Mensagem sem o atributo não veio do nosso publicador. Apagar
            // seria esconder o problema; deixar na fila leva à fila morta,
            // onde dá para olhar depois.
            log.warn("Mensagem sem o atributo {} na fila {}, deixando para a fila morta",
                    TransporteSns.ATRIBUTO_TOPICO, NomesNaAws.daFila(grupo));
            return false;
        }

        try {
            despachante.despachar(topico, mensagem.body());
        } catch (RuntimeException erro) {
            log.error("Consumo falhou em {} na tentativa {}, mensagem volta para a fila",
                    topico, tentativa(mensagem), erro);
            return false;
        }

        sqs.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(urlDaFila)
                .receiptHandle(mensagem.receiptHandle())
                .build());
        return true;
    }

    private static String atributo(Message mensagem, String nome) {
        var valor = mensagem.messageAttributes().get(nome);
        return valor == null ? null : valor.stringValue();
    }

    private static String tentativa(Message mensagem) {
        return mensagem.attributes()
                .getOrDefault(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT, "?");
    }

    private void dormirUmPouco() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException interrompida) {
            Thread.currentThread().interrupt();
            rodando.set(false);
        }
    }

    @Override
    public void stop() {
        rodando.set(false);
        if (thread != null) {
            thread.shutdownNow();
            thread = null;
        }
    }

    @Override
    public boolean isRunning() {
        return rodando.get();
    }

    /**
     * Sobe depois dos outros componentes e desce antes deles.
     *
     * <p>Mesma fase do ouvinte do Kafka, e pelo mesmo motivo: começar a
     * consumir antes de o resto do contexto estar pronto entrega evento para
     * um consumidor cuja dependência ainda não subiu.
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    void definirUrlDaFilaParaTeste(String url) {
        this.urlDaFila = url;
    }
}
