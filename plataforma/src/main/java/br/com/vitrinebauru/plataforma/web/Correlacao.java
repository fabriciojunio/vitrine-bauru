package br.com.vitrinebauru.plataforma.web;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Lê a correlação da requisição atual para carimbar no evento.
 *
 * <p>Fica aqui, e não dentro do filtro, porque quem precisa dela é o serviço
 * de domínio na hora de criar o evento, e o domínio não deve conhecer
 * {@code HttpServletRequest}.
 */
public final class Correlacao {

    private Correlacao() {
    }

    /**
     * @return a correlação da requisição, ou uma nova quando não há requisição
     *         (tarefa agendada, consumo de evento, teste). Nunca devolve nulo:
     *         evento sem correlação quebra o rastro justamente no caminho
     *         assíncrono, que é onde ele é mais necessário.
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
