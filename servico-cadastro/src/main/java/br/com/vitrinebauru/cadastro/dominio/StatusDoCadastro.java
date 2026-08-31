package br.com.vitrinebauru.cadastro.dominio;

/**
 * Onde o empreendedor esta na fila da SEDECON.
 *
 * <p>Este enum e o mecanismo de moderacao inteiro. Ninguem aparece na vitrine
 * publica sem passar por {@link #APROVADO}, e isso nao e burocracia: a
 * plataforma leva o nome da prefeitura, e um golpe aplicado por um cadastro
 * falso queimaria a confianca que a SEDECON levou anos construindo com o
 * Banco do Povo e a Casa do Empreendedor.
 */
public enum StatusDoCadastro {

    /** Cadastro enviado, esperando alguem da SEDECON olhar. */
    PENDENTE,

    /** No ar, visivel para o consumidor. */
    APROVADO,

    /**
     * Recusado com motivo escrito. Nao e fim de linha: o empreendedor corrige
     * o que foi apontado e reenvia, voltando para PENDENTE.
     */
    REJEITADO,

    /** Estava no ar e saiu, por denuncia ou a pedido do proprio dono. */
    SUSPENSO,

    /** Pediu exclusao de dados. Estado final, sem volta. */
    EXCLUIDO;

    public boolean apareceNaVitrine() {
        return this == APROVADO;
    }

    public boolean permiteEditarCatalogo() {
        return this == APROVADO || this == PENDENTE;
    }
}
