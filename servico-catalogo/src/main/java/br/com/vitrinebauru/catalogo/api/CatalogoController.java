package br.com.vitrinebauru.catalogo.api;

import br.com.vitrinebauru.catalogo.aplicacao.CuidarDoCatalogo;
import br.com.vitrinebauru.catalogo.dominio.Produto;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.CategoriaRepository;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.ImagemRepository;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.ProdutoRepository;
import br.com.vitrinebauru.contratos.tipos.Dinheiro;
import br.com.vitrinebauru.plataforma.seguranca.UsuarioAutenticado;
import br.com.vitrinebauru.plataforma.web.Pagina;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O painel do empreendedor sobre o proprio catalogo.
 *
 * <p>O dono sai do token, nunca da URL. Um endereco como
 * {@code /empreendedores/{id}/produtos} obrigaria a conferir, a cada metodo,
 * se o id da URL e o mesmo de quem esta logado, e um dia alguem esqueceria.
 */
@RestController
@RequestMapping("/api/catalogo")
@Tag(name = "Catálogo", description = "Produtos e serviços de cada loja")
public class CatalogoController {

    private static final int TAMANHO_MAXIMO_DA_PAGINA = 60;

    private final CuidarDoCatalogo catalogo;
    private final ProdutoRepository produtos;
    private final CategoriaRepository categorias;
    private final ImagemRepository imagens;

    public CatalogoController(CuidarDoCatalogo catalogo, ProdutoRepository produtos,
                              CategoriaRepository categorias,
                              ImagemRepository imagens) {
        this.catalogo = catalogo;
        this.produtos = produtos;
        this.categorias = categorias;
        this.imagens = imagens;
    }

    @GetMapping("/categorias")
    @Operation(summary = "Categorias disponíveis para classificar um produto")
    public List<Map<String, Object>> categorias() {
        return categorias.findAllByOrderByOrdemAsc().stream()
                .map(categoria -> Map.<String, Object>of(
                        "id", categoria.id(),
                        "nome", categoria.nome(),
                        "slug", categoria.slug()))
                .toList();
    }

    @GetMapping("/meus-produtos")
    @Operation(summary = "Produtos da loja de quem está logado")
    public Pagina<ProdutoResposta> meusProdutos(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {

        var paginacao = PageRequest.of(Math.max(pagina, 0),
                Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO_DA_PAGINA));

        return Pagina.de(produtos.findByEmpreendedorIdAndRetiradoEmIsNullOrderByCriadoEmDesc(
                exigirLoja(autenticado), paginacao), ProdutoResposta::de);
    }

    @PostMapping("/meus-produtos")
    @Operation(summary = "Publicar um produto ou serviço")
    public ResponseEntity<ProdutoResposta> publicar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody ProdutoRequisicao pedido) {

        var produto = catalogo.publicar(exigirLoja(autenticado), pedido.paraPedido());
        return ResponseEntity.created(java.net.URI.create("/api/catalogo/meus-produtos/" + produto.id()))
                .body(ProdutoResposta.de(produto));
    }

    @PutMapping("/meus-produtos/{produtoId}")
    @Operation(summary = "Alterar um produto")
    public ProdutoResposta alterar(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                                   @PathVariable UUID produtoId,
                                   @Valid @RequestBody ProdutoRequisicao pedido) {
        return ProdutoResposta.de(
                catalogo.alterar(exigirLoja(autenticado), produtoId, pedido.paraPedido()));
    }

    @PutMapping("/meus-produtos/{produtoId}/disponibilidade")
    @Operation(summary = "Marcar como disponível ou esgotado, sem apagar o produto")
    public ProdutoResposta disponibilidade(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                                           @PathVariable UUID produtoId,
                                           @RequestParam boolean disponivel) {
        return ProdutoResposta.de(
                catalogo.alternarDisponibilidade(exigirLoja(autenticado), produtoId, disponivel));
    }

    @PostMapping(value = "/meus-produtos/{produtoId}/imagem",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enviar a foto do produto")
    public ProdutoResposta enviarImagem(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                                        @PathVariable UUID produtoId,
                                        @RequestParam("arquivo") MultipartFile arquivo) {
        try {
            return ProdutoResposta.de(
                    catalogo.trocarImagem(exigirLoja(autenticado), produtoId, arquivo.getBytes()));
        } catch (IOException e) {
            throw new ErrosDeNegocio.RegraDeNegocio(
                    "Não foi possível ler o arquivo enviado. Tente de novo.");
        }
    }

    @DeleteMapping("/meus-produtos/{produtoId}")
    @Operation(summary = "Retirar o produto do catálogo")
    public ResponseEntity<Void> retirar(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                                        @PathVariable UUID produtoId) {
        catalogo.retirar(exigirLoja(autenticado), produtoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * A imagem e publica: ela aparece na vitrine, que nao tem login. O que
     * protege e o cabecalho de tipo, que sai do que foi detectado nos bytes, e
     * nao do que o navegador declarou no envio.
     */
    @GetMapping("/imagens/{imagemId}")
    @Operation(summary = "Baixar a foto de um produto")
    public ResponseEntity<byte[]> imagem(@PathVariable UUID imagemId) {
        var imagem = imagens.findById(imagemId).orElseThrow(() ->
                new ErrosDeNegocio.NaoEncontrado("Imagem não encontrada."));

        return ResponseEntity.ok()
                .header("Content-Type", imagem.tipo().tipoMime())
                .header("Cache-Control", "public, max-age=604800, immutable")
                .header("Content-Disposition", "inline")
                .header("X-Content-Type-Options", "nosniff")
                .body(imagem.conteudo());
    }

    private UUID exigirLoja(UsuarioAutenticado autenticado) {
        if (autenticado == null || autenticado.empreendedorId() == null) {
            throw new ErrosDeNegocio.Proibido("Sua conta não tem uma loja vinculada.");
        }
        return autenticado.empreendedorId();
    }

    public record ProdutoRequisicao(
            @NotBlank(message = "Escreva o nome do produto ou serviço")
            @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres")
            String nome,

            @Size(max = 800, message = "A descrição pode ter no máximo 800 caracteres")
            String descricao,

            @PositiveOrZero(message = "O preço não pode ser negativo")
            Long precoEmCentavos,

            @NotBlank(message = "Escolha a categoria")
            String categoria) {

        CuidarDoCatalogo.Pedido paraPedido() {
            return new CuidarDoCatalogo.Pedido(nome, descricao, precoEmCentavos, categoria);
        }
    }

    public record ProdutoResposta(UUID id, String nome, String descricao, Long precoEmCentavos,
                                  String precoFormatado, UUID categoriaId, String imagemUrl,
                                  boolean disponivel, Instant criadoEm) {

        static ProdutoResposta de(Produto produto) {
            return new ProdutoResposta(
                    produto.id(),
                    produto.nome(),
                    produto.descricao(),
                    produto.precoEmCentavos(),
                    produto.temPreco() ? produto.preco().formatado() : "Sob consulta",
                    produto.categoriaId(),
                    produto.imagemId() == null ? null : "/api/catalogo/imagens/" + produto.imagemId(),
                    produto.disponivel(),
                    produto.criadoEm());
        }
    }
}
