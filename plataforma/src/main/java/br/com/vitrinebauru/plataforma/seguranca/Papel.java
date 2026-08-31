package br.com.vitrinebauru.plataforma.seguranca;

/**
 * O que a pessoa pode fazer na plataforma.
 *
 * <p>São dois papéis porque o sistema tem dois lados e não mais que isso: quem
 * vende e quem modera. O consumidor não tem papel porque não tem conta, e essa
 * decisão é de produto: obrigar cadastro para ver a vitrine afastaria
 * exatamente o público que a SEDECON quer alcançar.
 */
public enum Papel {

    EMPREENDEDOR,
    ADMIN_SEDECON;

    /** Formato que o Spring Security espera em {@code hasRole}. */
    public String comoAutoridade() {
        return "ROLE_" + name();
    }
}
