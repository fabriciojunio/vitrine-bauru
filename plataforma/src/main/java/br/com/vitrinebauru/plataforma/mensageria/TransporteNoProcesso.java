package br.com.vitrinebauru.plataforma.mensageria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Transporte da implantacao gratuita: entrega dentro do proprio processo.
 *
 * <p>Existe porque nao ha Kafka gerenciado gratuito em 2026, e a demonstracao
 * que a SEDECON vai abrir precisa custar zero. Aqui os quatro servicos rodam
 * num processo so e a mensagem vai do outbox direto para o despachante.
 *
 * <p>O que continua valendo: a mensagem so sai depois de gravada no outbox, na
 * mesma transacao do estado; o consumidor continua idempotente pelo inbox; e
 * uma falha volta como excecao para o publicador, que conta a tentativa e
 * tenta de novo com espera crescente. O que se perde e o que se espera perder
 * sem broker: nao ha buffer durando alem do processo, nao ha consumo em
 * paralelo por particao, e a entrega e sincrona.
 *
 * <p>Quando o consumidor nao esta neste processo, a mensagem e descartada
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
