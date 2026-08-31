package br.com.vitrinebauru.catalogo.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * O que o catalogo sabe sobre um empreendedor, aprendido por evento.
 *
 * <p>O catalogo precisa responder duas perguntas antes de aceitar um produto:
 * este empreendedor existe, e ele pode publicar. Perguntar isso ao servico de
 * cadastro a cada produto salvo faria os dois servicos caírem juntos e
 * transformaria a separacao em enfeite. Guardando aqui o pouco que interessa,
 * o catalogo continua de pe mesmo com o cadastro fora do ar.
 *
 * <p>Nao ha documento, e-mail nem telefone nesta copia. Replicar dado pessoal
 * por comodidade e o jeito mais comum de espalhar informacao sensivel por
 * bancos que ninguem lembra de limpar depois.
 */
@Entity
@Table(name = "empreendedor_conhecido", schema = "catalogo")
public class EmpreendedorConhecido {

    @Id
    private UUID id;

    @Column(name = "nome_do_negocio", nullable = false, length = 120)
    private String nomeDoNegocio;

    @Column(nullable = false)
    private boolean podePublicar;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected EmpreendedorConhecido() {
    }

    public EmpreendedorConhecido(UUID id, String nomeDoNegocio, boolean podePublicar, Instant agora) {
        this.id = id;
        this.nomeDoNegocio = nomeDoNegocio;
        this.podePublicar = podePublicar;
        this.atualizadoEm = agora;
    }

    public void atualizar(String nomeDoNegocio, boolean podePublicar, Instant agora) {
        this.nomeDoNegocio = nomeDoNegocio;
        this.podePublicar = podePublicar;
        this.atualizadoEm = agora;
    }

    public UUID id() {
        return id;
    }

    public String nomeDoNegocio() {
        return nomeDoNegocio;
    }

    public boolean podePublicar() {
        return podePublicar;
    }

    public Instant atualizadoEm() {
        return atualizadoEm;
    }
}
