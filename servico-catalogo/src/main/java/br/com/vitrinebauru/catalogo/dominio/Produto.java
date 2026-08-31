package br.com.vitrinebauru.catalogo.dominio;

import br.com.vitrinebauru.contratos.tipos.Dinheiro;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * O que o empreendedor vende.
 *
 * <p>Duas decisoes de produto viram regra aqui dentro.
 *
 * <p>A primeira e o preco opcional. Marceneiro que faz movel sob medida,
 * costureira que ajusta vestido de festa e eletricista que precisa ver o
 * problema antes nao tem preco de tabela, e obrigar um numero faria os tres
 * inventarem um. Sem preco, a vitrine mostra "sob consulta", que e a verdade.
 *
 * <p>A segunda e a diferenca entre indisponivel e retirado. Bolo de pote que
 * acabou hoje volta amanha; obrigar o empreendedor a apagar e recadastrar o
 * produto a cada fim de estoque garantiria que ele parasse de atualizar.
 */
@Entity
@Table(name = "produto", schema = "catalogo")
public class Produto {

    private static final int TAMANHO_MAXIMO_DO_NOME = 120;

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

    @Column(name = "categoria_id", nullable = false)
    private UUID categoriaId;

    @Column(name = "imagem_id")
    private UUID imagemId;

    @Column(nullable = false)
    private boolean disponivel;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "retirado_em")
    private Instant retiradoEm;

    protected Produto() {
    }

    public static Produto novo(UUID empreendedorId, String nome, String descricao,
                               Dinheiro preco, UUID categoriaId, Instant agora) {
        return comId(UUID.randomUUID(), empreendedorId, nome, descricao, preco, categoriaId, agora);
    }

    /** Ver a semeadura da demonstracao: precisa dos mesmos ids nos servicos. */
    public static Produto comId(UUID id, UUID empreendedorId, String nome, String descricao,
                                Dinheiro preco, UUID categoriaId, Instant agora) {
        Produto produto = new Produto();
        produto.id = id;
        produto.empreendedorId = empreendedorId;
        produto.nome = exigirNome(nome);
        produto.descricao = descricao;
        produto.precoEmCentavos = preco == null ? null : preco.centavos();
        produto.categoriaId = categoriaId;
        produto.disponivel = true;
        produto.criadoEm = agora;
        produto.atualizadoEm = agora;
        return produto;
    }

    public void alterar(String nome, String descricao, Dinheiro preco, UUID categoriaId, Instant agora) {
        exigirNaVitrine();
        this.nome = exigirNome(nome);
        this.descricao = descricao;
        this.precoEmCentavos = preco == null ? null : preco.centavos();
        this.categoriaId = categoriaId;
        this.atualizadoEm = agora;
    }

    public void trocarImagem(UUID imagemId, Instant agora) {
        exigirNaVitrine();
        this.imagemId = imagemId;
        this.atualizadoEm = agora;
    }

    public void marcarDisponivel(boolean disponivel, Instant agora) {
        exigirNaVitrine();
        this.disponivel = disponivel;
        this.atualizadoEm = agora;
    }

    public void retirar(Instant agora) {
        if (retiradoEm != null) {
            throw new ErrosDeNegocio.Conflito("Este produto já foi retirado do catálogo.");
        }
        this.retiradoEm = agora;
        this.disponivel = false;
        this.atualizadoEm = agora;
    }

    public boolean foiRetirado() {
        return retiradoEm != null;
    }

    /** Aparece na busca publica so o que esta no catalogo e disponivel. */
    public boolean apareceNaVitrine() {
        return !foiRetirado() && disponivel;
    }

    public boolean pertenceA(UUID outroEmpreendedorId) {
        return empreendedorId.equals(outroEmpreendedorId);
    }

    private void exigirNaVitrine() {
        if (foiRetirado()) {
            throw new ErrosDeNegocio.Conflito(
                    "Este produto foi retirado do catálogo e não pode mais ser alterado.");
        }
    }

    private static String exigirNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ErrosDeNegocio.RegraDeNegocio("Escreva o nome do produto ou serviço.");
        }
        if (nome.length() > TAMANHO_MAXIMO_DO_NOME) {
            throw new ErrosDeNegocio.RegraDeNegocio(
                    "O nome do produto pode ter no máximo " + TAMANHO_MAXIMO_DO_NOME + " caracteres.");
        }
        return nome.trim();
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

    public Dinheiro preco() {
        return precoEmCentavos == null ? null : Dinheiro.deCentavos(precoEmCentavos);
    }

    public boolean temPreco() {
        return precoEmCentavos != null;
    }

    public UUID categoriaId() {
        return categoriaId;
    }

    public UUID imagemId() {
        return imagemId;
    }

    public boolean disponivel() {
        return disponivel;
    }

    public Instant criadoEm() {
        return criadoEm;
    }

    public Instant atualizadoEm() {
        return atualizadoEm;
    }

    public Instant retiradoEm() {
        return retiradoEm;
    }
}
