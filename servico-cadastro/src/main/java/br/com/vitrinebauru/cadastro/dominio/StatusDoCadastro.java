package br.com.vitrinebauru.cadastro.dominio;

/**
 * Onde o empreendedor está na fila da SEDECON.
 *
 * <p>Este enum é o mecanismo de moderação inteiro. Ninguém aparece na vitrine
 * pública sem passar por {@link #APROVADO}, e isso não é burocracia: a
 * plataforma leva o nome da prefeitura, e um golpe aplicado por um cadastro
 * falso queimaria a confiança que a SEDECON levou anos construindo com o
 * Banco do Povo e a Casa do Empreendedor.
 */
public enum StatusDoCadastro {

    /** Cadastro enviado, esperando alguém da SEDECON olhar. */
    PENDENTE,

    /** No ar, visível para o consumidor. */
    APROVADO,

    /**
     * Recusado com motivo escrito. Não é fim de linha: o empreendedor corrige
     * o que foi apontado e reenvia, voltando para PENDENTE.
     */
    REJEITADO,

    /** Estava no ar e saiu, por denúncia ou a pedido do próprio dono. */
    SUSPENSO,

    /** Pediu exclusão de dados. Estado final, sem volta. */
    EXCLUIDO;

    public boolean apareceNaVitrine() {
        return this == APROVADO;
    }

    public boolean permiteEditarCatalogo() {
        return this == APROVADO || this == PENDENTE;
    }
}
