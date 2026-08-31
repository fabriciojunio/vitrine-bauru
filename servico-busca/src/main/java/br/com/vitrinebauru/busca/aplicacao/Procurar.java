package br.com.vitrinebauru.busca.aplicacao;

import br.com.vitrinebauru.busca.dominio.LojaNaVitrine;
import br.com.vitrinebauru.busca.dominio.Normalizacao;
import br.com.vitrinebauru.busca.dominio.ProdutoNaVitrine;
import br.com.vitrinebauru.busca.infraestrutura.persistencia.LojaRepository;
import br.com.vitrinebauru.busca.infraestrutura.persistencia.ProdutoDaVitrineRepository;
import br.com.vitrinebauru.contratos.BairrosDeBauru;
import br.com.vitrinebauru.contratos.CategoriasDoComercio;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * A busca publica: por palavra, por bairro e por categoria.
 *
 * <p>Os tres filtros sao opcionais e se combinam. Sem nenhum, a resposta e a
 * vitrine inteira, que e o que a pagina inicial mostra: quem chega pela
 * primeira vez nao sabe o que procurar, e uma tela pedindo termo de busca
 * antes de mostrar qualquer coisa e uma tela vazia.
 *
 * <p>O bairro e a categoria sao conferidos contra a lista oficial. Alem de
 * evitar filtro sem resultado por causa de acento, isso impede que um valor
 * qualquer da URL chegue a consulta.
 */
@Component
public class Procurar {

    private static final int TAMANHO_MAXIMO_DA_PAGINA = 48;
    private static final int TAMANHO_MINIMO_DO_TERMO = 2;

    private final ProdutoDaVitrineRepository produtos;
    private final LojaRepository lojas;

    public Procurar(ProdutoDaVitrineRepository produtos, LojaRepository lojas) {
        this.produtos = produtos;
        this.lojas = lojas;
    }

    @Transactional(readOnly = true)
    public Page<ProdutoNaVitrine> produtos(Filtro filtro, int pagina, int tamanho) {
        return produtos.procurar(
                termoDeBusca(filtro.termo()),
                bairroValido(filtro.bairro()),
                categoriaValida(filtro.categoria()),
                filtro.precoMaximoEmCentavos(),
                paginacao(pagina, tamanho));
    }

    @Transactional(readOnly = true)
    public Page<LojaNaVitrine> lojas(Filtro filtro, int pagina, int tamanho) {
        return lojas.procurar(
                termoDeBusca(filtro.termo()),
                bairroValido(filtro.bairro()),
                categoriaValida(filtro.categoria()),
                paginacao(pagina, tamanho));
    }

    @Transactional(readOnly = true)
    public LojaComProdutos loja(String apelidoNaUrl) {
        var loja = lojas.findByApelidoNaUrlAndVisivelIsTrue(apelidoNaUrl).orElseThrow(() ->
                new ErrosDeNegocio.NaoEncontrado("Essa loja não está disponível."));

        return new LojaComProdutos(loja,
                produtos.findByEmpreendedorIdAndVisivelIsTrueAndDisponivelIsTrue(loja.id()));
    }

    @Transactional(readOnly = true)
    public Resumo resumo() {
        return new Resumo(
                lojas.countByVisivelIsTrue(),
                produtos.countByVisivelIsTrueAndDisponivelIsTrue(),
                lojas.bairrosComLoja(),
                lojas.categoriasComLoja());
    }

    /**
     * Termo com uma letra so nao filtra nada util e faz varrer a tabela inteira
     * a cada tecla digitada. Abaixo do minimo, o termo e ignorado.
     */
    private String termoDeBusca(String termo) {
        if (termo == null || Normalizacao.paraBusca(termo).length() < TAMANHO_MINIMO_DO_TERMO) {
            return null;
        }
        return Normalizacao.paraBusca(termo);
    }

    private String bairroValido(String bairro) {
        if (bairro == null || bairro.isBlank()) {
            return null;
        }
        return BairrosDeBauru.normalizado(bairro).orElseThrow(() ->
                new ErrosDeNegocio.RegraDeNegocio("Bairro não reconhecido em Bauru."));
    }

    private String categoriaValida(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return null;
        }
        return CategoriasDoComercio.normalizada(categoria).orElseThrow(() ->
                new ErrosDeNegocio.RegraDeNegocio("Categoria não reconhecida."));
    }

    private PageRequest paginacao(int pagina, int tamanho) {
        return PageRequest.of(Math.max(pagina, 0),
                Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO_DA_PAGINA));
    }

    public record Filtro(String termo, String bairro, String categoria, Long precoMaximoEmCentavos) {
    }

    public record LojaComProdutos(LojaNaVitrine loja, List<ProdutoNaVitrine> produtos) {
    }

    public record Resumo(long lojas, long produtos, List<String> bairros, List<String> categorias) {
    }
}
