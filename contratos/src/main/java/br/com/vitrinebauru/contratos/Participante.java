package br.com.vitrinebauru.contratos;

import java.util.Set;

/**
 * Quem precisa confirmar o expurgo antes da saga de exclusao fechar.
 *
 * <p>A lista mora no contrato, e nao no coordenador, porque acrescentar um
 * servico que guarda dado pessoal e mexer no contrato: quem esquecer de
 * incluir o servico novo aqui vai fechar saga com dado vivo em algum banco.
 */
public enum Participante {
    CATALOGO,
    BUSCA,
    NOTIFICACOES;

    public static Set<Participante> todos() {
        return Set.of(values());
    }
}
