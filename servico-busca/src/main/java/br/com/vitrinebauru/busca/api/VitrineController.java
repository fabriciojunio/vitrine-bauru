package br.com.vitrinebauru.busca.api;

import br.com.vitrinebauru.busca.aplicacao.Procurar;
import br.com.vitrinebauru.busca.aplicacao.RegistrarContato;
import br.com.vitrinebauru.busca.dominio.LojaNaVitrine;
import br.com.vitrinebauru.busca.dominio.ProdutoNaVitrine;
import br.com.vitrinebauru.contratos.CanalDeContato;
import br.com.vitrinebauru.contratos.OrigemDoContato;
import br.com.vitrinebauru.contratos.tipos.Dinheiro;
import br.com.vitrinebauru.contratos.tipos.Telefone;
import br.com.vitrinebauru.plataforma.web.Pagina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * A vitrine publica. Nenhum endereco daqui pede login.
 *
 * <p>Essa e a decisao de produto mais importante do projeto: obrigar cadastro
 * para ver o que a padaria do bairro vende afastaria o consumidor e, com ele,
 * o empreendedor. O que existe de conta na plataforma e do lado de quem vende.
 */
@RestController
@RequestMapping("/api/busca")
@Tag(name = "Vitrine", description = "Busca pública de produtos e lojas")
public class VitrineController {

    private final Procurar procurar;
    private final RegistrarContato registrarContato;

    public VitrineController(Procurar procurar, RegistrarContato registrarContato) {
        this.procurar = procurar;
        this.registrarContato = registrarContato;
    }

    @GetMapping("/produtos")
    @Operation(summary = "Procurar produtos por palavra, bairro, categoria e preço")
    public Pagina<ProdutoResposta> produtos(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) String bairro,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Long precoMaximoEmCentavos,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "24") int tamanho) {

        var filtro = new Procurar.Filtro(termo, bairro, categoria, precoMaximoEmCentavos);
        return Pagina.de(procurar.produtos(filtro, pagina, tamanho), ProdutoResposta::de);
    }

    @GetMapping("/lojas")
    @Operation(summary = "Procurar lojas por palavra, bairro e categoria")
    public Pagina<LojaResposta> lojas(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) String bairro,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "24") int tamanho) {

        var filtro = new Procurar.Filtro(termo, bairro, categoria, null);
        return Pagina.de(procurar.lojas(filtro, pagina, tamanho), LojaResposta::de);
    }

    @GetMapping("/lojas/{apelidoNaUrl}")
    @Operation(summary = "Página pública de uma loja, com o catálogo dela")
    public LojaCompleta loja(@PathVariable String apelidoNaUrl) {
        var encontrada = procurar.loja(apelidoNaUrl);

        return new LojaCompleta(
                LojaResposta.de(encontrada.loja()),
                encontrada.produtos().stream().map(ProdutoResposta::de).toList());
    }

    @GetMapping("/resumo")
    @Operation(summary = "Números da vitrine e filtros disponíveis")
    public Procurar.Resumo resumo() {
        return procurar.resumo();
    }

    /**
     * Registra a intencao de contato e devolve o link do WhatsApp.
     *
     * <p>E POST porque muda estado (grava um evento), mesmo sendo disparado por
     * um clique num link. O frontend abre o link do WhatsApp direto na
     * ancora e manda este aviso em paralelo, para o bloqueador de janela do
     * navegador nao atrapalhar o unico fluxo que importa.
     */
    @PostMapping("/contatos")
    @Operation(summary = "Registrar o clique em falar no WhatsApp")
    public RegistrarContato.Resultado contato(@Valid @RequestBody ContatoRequisicao pedido) {
        return registrarContato.executar(
                pedido.empreendedorId(),
                pedido.produtoId(),
                pedido.nomeDoProduto(),
                pedido.canal() == null ? CanalDeContato.WHATSAPP : pedido.canal(),
                pedido.origem() == null ? OrigemDoContato.RESULTADO_DA_BUSCA : pedido.origem());
    }

    public record ContatoRequisicao(
            @NotNull(message = "Informe a loja") UUID empreendedorId,
            UUID produtoId,
            String nomeDoProduto,
            CanalDeContato canal,
            OrigemDoContato origem) {
    }

    public record ProdutoResposta(UUID id, String nome, String descricao, Long precoEmCentavos,
                                  String precoFormatado, String categoria, String imagemUrl,
                                  UUID empreendedorId, String lojaNome, String lojaApelido,
                                  String bairro) {

        static ProdutoResposta de(ProdutoNaVitrine produto) {
            return new ProdutoResposta(
                    produto.id(),
                    produto.nome(),
                    produto.descricao(),
                    produto.precoEmCentavos(),
                    produto.precoEmCentavos() == null
                            ? "Sob consulta"
                            : Dinheiro.deCentavos(produto.precoEmCentavos()).formatado(),
                    produto.categoriaNome(),
                    produto.imagemUrl(),
                    produto.empreendedorId(),
                    produto.lojaNome(),
                    produto.lojaApelido(),
                    produto.bairro());
        }
    }

    public record LojaResposta(UUID id, String nomeDoNegocio, String apelidoNaUrl, String descricao,
                               String categoria, String bairro, String telefoneWhatsapp,
                               String fotoDeCapaUrl) {

        static LojaResposta de(LojaNaVitrine loja) {
            return new LojaResposta(
                    loja.id(),
                    loja.nomeDoNegocio(),
                    loja.apelidoNaUrl(),
                    loja.descricao(),
                    loja.categoria(),
                    loja.bairro(),
                    Telefone.de(loja.telefoneWhatsapp()).formatado(),
                    loja.fotoDeCapaUrl());
        }
    }

    public record LojaCompleta(LojaResposta loja, List<ProdutoResposta> produtos) {
    }
}
