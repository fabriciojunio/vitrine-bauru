package br.com.vitrinebauru.plataforma.web;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Le a correlacao da requisicao atual para carimbar no evento.
 *
 * <p>Fica aqui, e nao dentro do filtro, porque quem precisa dela e o servico
 * de dominio na hora de criar o evento, e o dominio nao deve conhecer
 * {@code HttpServletRequest}.
 */
public final class Correlacao {

    private Correlacao() {
    }

    /**
     * @return a correlacao da requisicao, ou uma nova quando nao ha requisicao
     *         (tarefa agendada, consumo de evento, teste). Nunca devolve nulo:
     *         evento sem correlacao quebra o rastro justamente no caminho
     *         assincrono, que e onde ele e mais necessario.
     */
    public static UUID atual() {
        String valor = MDC.get(FiltroDeCorrelacao.CHAVE_NO_LOG);
        if (valor == null) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }
}
