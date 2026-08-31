package br.com.vitrinebauru.contratos;

import java.util.Set;

/**
 * Quem precisa confirmar o expurgo antes da saga de exclusão fechar.
 *
 * <p>A lista mora no contrato, e não no coordenador, porque acrescentar um
 * serviço que guarda dado pessoal é mexer no contrato: quem esquecer de
 * incluir o serviço novo aqui vai fechar saga com dado vivo em algum banco.
 */
public enum Participante {
    CATALOGO,
    BUSCA,
    NOTIFICACOES;

    public static Set<Participante> todos() {
        return Set.of(values());
    }
}
