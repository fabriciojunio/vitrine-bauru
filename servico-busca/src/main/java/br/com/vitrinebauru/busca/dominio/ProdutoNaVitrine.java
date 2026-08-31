package br.com.vitrinebauru.busca.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * O produto como o consumidor o ve.
 *
 * <p>Guarda junto o bairro e o nome da loja, que sao dados do cadastro e nao
 * do catalogo. Isso e duplicacao de proposito: sem ela, filtrar produto por
 * bairro exigiria juntar duas tabelas alimentadas por servicos diferentes a
 * cada busca. Como projecao pode ser reconstruida a partir dos eventos, a
 * duplicacao aqui nao cria uma segunda fonte da verdade.
 *
 * <p>Quando o produto chega antes da loja (a ordem entre topicos diferentes
 * nao e garantida), a linha e criada mesmo assim, com o nome da loja em
 * branco, e completada quando o evento da loja chegar. Melhor uma projecao que
 * se completa sozinha do que uma que rejeita o evento e some com o produto.
 */
@Entity
@Table(name = "produto", schema = "busca")
public class ProdutoNaVitrine {

    @Id
    private UUID id;

    @Column(name = "empreendedor_id", nullable = false)
    private UUID empreendedorId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 800)
    private String descricao;

    @Column(name = "preco_em_centavos")
    private Long precoEmCentavos;

    @Column(name = "categoria_nome", nullable = false, length = 60)
    private String categoriaNome;

    @Column(name = "imagem_url", length = 400)
    private String imagemUrl;

    @Column(nullable = false)
    private boolean disponivel;

    @Column(name = "loja_nome", length = 120)
    private String lojaNome;

    @Column(name = "loja_apelido", length = 60)
    private String lojaApelido;

    @Column(length = 60)
    private String bairro;

    @Column(nullable = false)
    private boolean visivel;

    @Column(nullable = false, length = 1100)
    private String busca;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected ProdutoNaVitrine() {
    }

    public static ProdutoNaVitrine novo(UUID id, UUID empreendedorId, Instant agora) {
        ProdutoNaVitrine produto = new ProdutoNaVitrine();
        produto.id = id;
        produto.empreendedorId = empreendedorId;
        produto.atualizadoEm = agora;
        produto.busca = "";
        produto.nome = "";
        produto.categoriaNome = "";
        return produto;
    }

    public void atualizarDoCatalogo(String nome, String descricao, Long precoEmCentavos,
                                    String categoriaNome, String imagemUrl, boolean disponivel,
                                    Instant agora) {
        this.nome = nome;
        this.descricao = descricao;
        this.precoEmCentavos = precoEmCentavos;
        this.categoriaNome = categoriaNome;
        this.imagemUrl = imagemUrl;
        this.disponivel = disponivel;
        this.atualizadoEm = agora;
        recalcularBusca();
    }

    public void atualizarDaLoja(String lojaNome, String lojaApelido, String bairro,
                                boolean lojaVisivel, Instant agora) {
        this.lojaNome = lojaNome;
        this.lojaApelido = lojaApelido;
        this.bairro = bairro;
        this.visivel = lojaVisivel;
        this.atualizadoEm = agora;
        recalcularBusca();
    }

    private void recalcularBusca() {
        this.busca = Normalizacao.juntar(nome, descricao, categoriaNome, lojaNome, bairro);
    }

    /** So aparece o que esta disponivel e cuja loja esta no ar. */
    public boolean apareceNaVitrine() {
        return visivel && disponivel;
    }

    public UUID id() {
        return id;
    }

    public UUID empreendedorId() {
        return empreendedorId;
    }

    public String nome() {
        return nome;
    }

    public String descricao() {
        return descricao;
    }

    public Long precoEmCentavos() {
        return precoEmCentavos;
    }

    public String categoriaNome() {
        return categoriaNome;
    }

    public String imagemUrl() {
        return imagemUrl;
    }

    public boolean disponivel() {
        return disponivel;
    }

    public String lojaNome() {
        return lojaNome;
    }

    public String lojaApelido() {
        return lojaApelido;
    }

    public String bairro() {
        return bairro;
    }

    public boolean visivel() {
        return visivel;
    }

    public String busca() {
        return busca;
    }

    public Instant atualizadoEm() {
        return atualizadoEm;
    }
}
