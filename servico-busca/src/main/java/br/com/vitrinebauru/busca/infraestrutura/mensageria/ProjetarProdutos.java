package br.com.vitrinebauru.busca.infraestrutura.mensageria;

import br.com.vitrinebauru.busca.dominio.ProdutoNaVitrine;
import br.com.vitrinebauru.busca.infraestrutura.persistencia.LojaRepository;
import br.com.vitrinebauru.busca.infraestrutura.persistencia.ProdutoDaVitrineRepository;
import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.contratos.ProdutoAtualizado;
import br.com.vitrinebauru.contratos.ProdutoPublicado;
import br.com.vitrinebauru.contratos.ProdutoRetirado;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.mensageria.ConsumidorDeEventos;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Projeta o catálogo na vitrine.
 *
 * <p>Trata o caso em que o produto chega antes da loja: os dois tópicos são
 * independentes e nada garante a ordem entre eles. Quando isso acontece, o
 * produto é gravado invisível e passa a aparecer quando o evento da loja
 * chegar e completar o nome e o bairro. O caminho contrário também funciona,
 * porque a projeção da loja espalha os dados nos produtos que já existem.
 */
@Component
public class ProjetarProdutos implements ConsumidorDeEventos {

    private final ProdutoDaVitrineRepository produtos;
    private final LojaRepository lojas;
    private final Clock relogio;

    public ProjetarProdutos(ProdutoDaVitrineRepository produtos, LojaRepository lojas, Clock relogio) {
        this.produtos = produtos;
        this.lojas = lojas;
        this.relogio = relogio;
    }

    @Override
    public String nome() {
        return "busca-projecao-de-produtos";
    }

    @Override
    public Set<String> topicos() {
        return Set.of(Topicos.CATALOGO);
    }

    @Override
    public void consumir(Evento evento) {
        var agora = relogio.instant();

        switch (evento) {
            case ProdutoPublicado publicado -> gravar(publicado.produtoId(), publicado.empreendedorId(),
                    publicado.nome(), publicado.descricao(), publicado.precoEmCentavos(),
                    publicado.categoriaNome(), publicado.imagemUrl(), publicado.disponivel(), agora);

            case ProdutoAtualizado atualizado -> gravar(atualizado.produtoId(), atualizado.empreendedorId(),
                    atualizado.nome(), atualizado.descricao(), atualizado.precoEmCentavos(),
                    atualizado.categoriaNome(), atualizado.imagemUrl(), atualizado.disponivel(), agora);

            case ProdutoRetirado retirado -> produtos.deleteById(retirado.produtoId());

            default -> {
                // Outros eventos do tópico não mudam a vitrine.
            }
        }
    }

    private void gravar(UUID produtoId, UUID empreendedorId, String nome, String descricao,
                        Long preco, String categoria, String imagemUrl, boolean disponivel,
                        Instant agora) {
        var produto = produtos.findById(produtoId)
                .orElseGet(() -> ProdutoNaVitrine.novo(produtoId, empreendedorId, agora));

        produto.atualizarDoCatalogo(nome, descricao, preco, categoria, imagemUrl, disponivel, agora);

        // Completa com o que se sabe da loja. Se ela ainda não chegou, o
        // produto fica invisível até o evento dela aparecer.
        lojas.findById(empreendedorId).ifPresent(loja ->
                produto.atualizarDaLoja(loja.nomeDoNegocio(), loja.apelidoNaUrl(),
                        loja.bairro(), loja.visivel(), agora));

        produtos.save(produto);
    }
}
