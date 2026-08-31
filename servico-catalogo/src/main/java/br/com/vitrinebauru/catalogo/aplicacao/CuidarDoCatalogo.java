package br.com.vitrinebauru.catalogo.aplicacao;

import br.com.vitrinebauru.catalogo.dominio.Categoria;
import br.com.vitrinebauru.catalogo.dominio.EmpreendedorConhecido;
import br.com.vitrinebauru.catalogo.dominio.Produto;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.CategoriaRepository;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.EmpreendedorConhecidoRepository;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.ImagemRepository;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.ProdutoRepository;
import br.com.vitrinebauru.contratos.ProdutoAtualizado;
import br.com.vitrinebauru.contratos.ProdutoPublicado;
import br.com.vitrinebauru.contratos.ProdutoRetirado;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.contratos.tipos.Dinheiro;
import br.com.vitrinebauru.plataforma.outbox.RegistroDeSaida;
import br.com.vitrinebauru.plataforma.texto.Sanitizador;
import br.com.vitrinebauru.plataforma.web.Correlacao;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * O catalogo do empreendedor: publicar, alterar, esconder e retirar produto.
 *
 * <p>Toda operacao confere duas coisas antes: se o empreendedor pode publicar
 * (o que ele so pode depois de a SEDECON aprovar ou enquanto espera analise) e
 * se o produto e dele. A segunda confere sempre, mesmo o identificador do dono
 * vindo do token, porque o identificador do produto vem da URL e URL e
 * chutavel.
 *
 * <p>Cada mudanca vira evento no mesmo commit. E o evento que atualiza a busca
 * publica; sem ele, o empreendedor veria o produto no painel dele e o
 * consumidor nao acharia nada.
 */
@Component
public class CuidarDoCatalogo {

    /**
     * Teto de espaco por loja. Existe porque a demonstracao roda em banco de
     * camada gratuita com 1 GB: sem limite, uma loja sozinha derruba a
     * plataforma inteira sem querer.
     */
    private static final long ESPACO_MAXIMO_POR_LOJA = 50L * 1024 * 1024;

    private final ProdutoRepository produtos;
    private final CategoriaRepository categorias;
    private final EmpreendedorConhecidoRepository empreendedores;
    private final ImagemRepository imagens;
    private final RegistroDeSaida outbox;
    private final Sanitizador sanitizador;
    private final Clock relogio;

    public CuidarDoCatalogo(ProdutoRepository produtos,
                            CategoriaRepository categorias,
                            EmpreendedorConhecidoRepository empreendedores,
                            ImagemRepository imagens,
                            RegistroDeSaida outbox, Sanitizador sanitizador, Clock relogio) {
        this.produtos = produtos;
        this.categorias = categorias;
        this.empreendedores = empreendedores;
        this.imagens = imagens;
        this.outbox = outbox;
        this.sanitizador = sanitizador;
        this.relogio = relogio;
    }

    @Transactional
    public Produto publicar(UUID empreendedorId, Pedido pedido) {
        exigirQuePodePublicar(empreendedorId);
        var categoria = categoriaPeloNome(pedido.categoria());
        var agora = relogio.instant();

        var produto = produtos.save(Produto.novo(
                empreendedorId,
                sanitizador.limpar(pedido.nome()),
                sanitizador.limpar(pedido.descricao()),
                precoDe(pedido.precoEmCentavos()),
                categoria.id(),
                agora));

        outbox.gravar(Topicos.CATALOGO, new ProdutoPublicado(
                UUID.randomUUID(), Correlacao.atual(), agora,
                produto.id(), empreendedorId, produto.nome(), produto.descricao(),
                produto.precoEmCentavos(), categoria.id(), categoria.nome(),
                enderecoDaImagem(produto), produto.disponivel()));

        return produto;
    }

    @Transactional
    public Produto alterar(UUID empreendedorId, UUID produtoId, Pedido pedido) {
        var produto = carregarDoDono(empreendedorId, produtoId);
        var categoria = categoriaPeloNome(pedido.categoria());
        var agora = relogio.instant();

        produto.alterar(
                sanitizador.limpar(pedido.nome()),
                sanitizador.limpar(pedido.descricao()),
                precoDe(pedido.precoEmCentavos()),
                categoria.id(),
                agora);

        publicarAtualizacao(produto, categoria, agora);
        return produto;
    }

    @Transactional
    public Produto alternarDisponibilidade(UUID empreendedorId, UUID produtoId, boolean disponivel) {
        var produto = carregarDoDono(empreendedorId, produtoId);
        var agora = relogio.instant();

        produto.marcarDisponivel(disponivel, agora);

        publicarAtualizacao(produto, categoriaPorId(produto.categoriaId()), agora);
        return produto;
    }

    @Transactional
    public Produto trocarImagem(UUID empreendedorId, UUID produtoId, byte[] conteudo) {
        var produto = carregarDoDono(empreendedorId, produtoId);
        var agora = relogio.instant();

        Long usado = imagens.espacoUsadoPor(empreendedorId);
        if (usado != null && usado + conteudo.length > ESPACO_MAXIMO_POR_LOJA) {
            throw new ErrosDeNegocio.RegraDeNegocio(
                    "Você já usou o espaço disponível para fotos. Apague alguma foto antiga antes.");
        }

        var imagem = imagens.save(br.com.vitrinebauru.catalogo.dominio.ImagemDeProduto
                .nova(empreendedorId, conteudo, agora));
        produto.trocarImagem(imagem.id(), agora);

        publicarAtualizacao(produto, categoriaPorId(produto.categoriaId()), agora);
        return produto;
    }

    @Transactional
    public void retirar(UUID empreendedorId, UUID produtoId) {
        var produto = carregarDoDono(empreendedorId, produtoId);
        var agora = relogio.instant();

        produto.retirar(agora);

        outbox.gravar(Topicos.CATALOGO, new ProdutoRetirado(
                UUID.randomUUID(), Correlacao.atual(), agora, produto.id(), empreendedorId));
    }

    private void publicarAtualizacao(Produto produto, Categoria categoria, Instant agora) {
        outbox.gravar(Topicos.CATALOGO, new ProdutoAtualizado(
                UUID.randomUUID(), Correlacao.atual(), agora,
                produto.id(), produto.empreendedorId(), produto.nome(), produto.descricao(),
                produto.precoEmCentavos(), categoria.id(), categoria.nome(),
                enderecoDaImagem(produto), produto.disponivel()));
    }

    /**
     * A imagem viaja como endereco, e nao como bytes. Evento com foto dentro
     * ficaria com megabytes por mensagem e entupiria o topico.
     */
    private String enderecoDaImagem(Produto produto) {
        return produto.imagemId() == null
                ? null
                : "/api/catalogo/imagens/" + produto.imagemId();
    }

    private Produto carregarDoDono(UUID empreendedorId, UUID produtoId) {
        var produto = produtos.findByIdAndRetiradoEmIsNull(produtoId).orElseThrow(() ->
                new ErrosDeNegocio.NaoEncontrado("Produto não encontrado."));

        if (!produto.pertenceA(empreendedorId)) {
            throw new ErrosDeNegocio.Proibido("Este produto é de outra loja.");
        }
        exigirQuePodePublicar(empreendedorId);
        return produto;
    }

    private void exigirQuePodePublicar(UUID empreendedorId) {
        EmpreendedorConhecido conhecido = empreendedores.findById(empreendedorId).orElseThrow(() ->
                new ErrosDeNegocio.NaoEncontrado(
                        "Sua loja ainda não chegou ao catálogo. Tente de novo em alguns instantes."));

        if (!conhecido.podePublicar()) {
            throw new ErrosDeNegocio.Proibido(
                    "Sua loja está suspensa ou excluída e não pode alterar o catálogo.");
        }
    }

    private Categoria categoriaPeloNome(String nome) {
        return categorias.findByNome(nome)
                .or(() -> categorias.findBySlug(nome == null ? "" : nome.toLowerCase()))
                .orElseThrow(() -> new ErrosDeNegocio.RegraDeNegocio(
                        "Categoria não reconhecida. Escolha uma da lista."));
    }

    private Categoria categoriaPorId(UUID id) {
        return categorias.findById(id).orElseThrow(() ->
                new ErrosDeNegocio.NaoEncontrado("Categoria não encontrada."));
    }

    private Dinheiro precoDe(Long centavos) {
        return centavos == null ? null : Dinheiro.deCentavos(centavos);
    }

    /**
     * @param precoEmCentavos nulo significa "sob consulta", e nao zero. Zero e
     *                        um preco valido: tem servico que a loja oferece de
     *                        graca, como o leva e traz do banho e tosa.
     */
    public record Pedido(String nome, String descricao, Long precoEmCentavos, String categoria) {
    }
}
