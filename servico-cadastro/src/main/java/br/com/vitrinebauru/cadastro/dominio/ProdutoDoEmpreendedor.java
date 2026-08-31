package br.com.vitrinebauru.cadastro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Copia magra do catalogo, mantida por evento.
 *
 * <p>Existe para o painel da SEDECON responder "quantos produtos ja foram
 * cadastrados" e, principalmente, "quem foi aprovado e nunca publicou nada"
 * sem consultar o servico de catalogo. Essa segunda pergunta e a mais util das
 * duas: e a lista de quem precisa de ajuda para usar a ferramenta, que e
 * exatamente o objetivo de capacitacao do projeto.
 *
 * <p>Guarda o minimo: id, dono e data. Nome, preco e foto continuam sendo do
 * catalogo, e replicar isso aqui seria criar uma segunda fonte da verdade para
 * o mesmo dado.
 */
@Entity
@Table(name = "produto_do_empreendedor", schema = "cadastro")
public class ProdutoDoEmpreendedor {

    @Id
    @Column(name = "produto_id")
    private UUID produtoId;

    @Column(name = "empreendedor_id", nullable = false)
    private UUID empreendedorId;

    @Column(name = "publicado_em", nullable = false)
    private Instant publicadoEm;

    protected ProdutoDoEmpreendedor() {
    }

    public ProdutoDoEmpreendedor(UUID produtoId, UUID empreendedorId, Instant publicadoEm) {
        this.produtoId = produtoId;
        this.empreendedorId = empreendedorId;
        this.publicadoEm = publicadoEm;
    }

    public UUID produtoId() {
        return produtoId;
    }

    public UUID empreendedorId() {
        return empreendedorId;
    }

    public Instant publicadoEm() {
        return publicadoEm;
    }
}
