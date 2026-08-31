package br.com.vitrinebauru.plataforma.seguranca;

/**
 * O que a pessoa pode fazer na plataforma.
 *
 * <p>Sao dois papeis porque o sistema tem dois lados e nao mais que isso: quem
 * vende e quem modera. O consumidor nao tem papel porque nao tem conta, e essa
 * decisao e de produto: obrigar cadastro para ver a vitrine afastaria
 * exatamente o publico que a SEDECON quer alcancar.
 */
public enum Papel {

    EMPREENDEDOR,
    ADMIN_SEDECON;

    /** Formato que o Spring Security espera em {@code hasRole}. */
    public String comoAutoridade() {
        return "ROLE_" + name();
    }
}
