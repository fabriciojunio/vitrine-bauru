package br.com.vitrinebauru.plataforma.mensageria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Transporte da implantação gratuita: entrega dentro do próprio processo.
 *
 * <p>Existe porque não há Kafka gerenciado gratuito em 2026, e a demonstração
 * que a SEDECON vai abrir precisa custar zero. Aqui os quatro serviços rodam
 * num processo só e a mensagem vai do outbox direto para o despachante.
 *
 * <p>O que continua valendo: a mensagem só sai depois de gravada no outbox, na
 * mesma transação do estado; o consumidor continua idempotente pelo inbox; e
 * uma falha volta como exceção para o publicador, que conta a tentativa e
 * tenta de novo com espera crescente. O que se perde é o que se espera perder
 * sem broker: não há buffer durando além do processo, não há consumo em
 * paralelo por partição, e a entrega é síncrona.
 *
 * <p>Quando o consumidor não está neste processo, a mensagem é descartada
 * com aviso no log em vez de ficar travada para sempre no outbox.
 */
@Component
@ConditionalOnProperty(name = "vitrine.mensageria.transporte", havingValue = "processo")
public class TransporteNoProcesso implements TransporteDeEventos {

    private static final Logger log = LoggerFactory.getLogger(TransporteNoProcesso.class);

    private final Despachante despachante;

    public TransporteNoProcesso(Despachante despachante) {
        this.despachante = despachante;
    }

    @Override
    public void enviar(String topico, String chave, String carga) {
        Set<String> assinados = despachante.topicosAssinados();
        if (!assinados.contains(topico)) {
            log.debug("Nenhum consumidor neste processo assina {}, mensagem entregue a ninguem", topico);
            return;
        }
        despachante.despachar(topico, carga);
    }

    @Override
    public String descricao() {
        return "processo";
    }
}
