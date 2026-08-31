package br.com.vitrinebauru.cadastro.infraestrutura.mensageria;

import br.com.vitrinebauru.cadastro.dominio.ProdutoDoEmpreendedor;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.ProdutoDoEmpreendedorRepository;
import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.contratos.ProdutoAtualizado;
import br.com.vitrinebauru.contratos.ProdutoPublicado;
import br.com.vitrinebauru.contratos.ProdutoRetirado;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.mensageria.ConsumidorDeEventos;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Mantém, no cadastro, a contagem de produtos de cada empreendedor.
 *
 * <p>O painel da SEDECON precisa saber quem publicou produto e quem não
 * publicou. Perguntar isso ao serviço de catálogo a cada abertura do painel
 * colocaria os dois serviços no mesmo caminho crítico: catálogo fora do ar,
 * painel fora do ar. Guardando por evento, o painel continua respondendo.
 *
 * <p>O preço é conhecido: entre a publicação do produto e a chegada do evento
 * a contagem fica velha por alguns segundos. Para uma pergunta como "quem
 * ainda não cadastrou nada", segundos de atraso não mudam nenhuma decisão.
 */
@Component
public class OuvinteDoCatalogo implements ConsumidorDeEventos {

    private final ProdutoDoEmpreendedorRepository produtos;

    public OuvinteDoCatalogo(ProdutoDoEmpreendedorRepository produtos) {
        this.produtos = produtos;
    }

    @Override
    public String nome() {
        return "cadastro-contador-de-produtos";
    }

    @Override
    public Set<String> topicos() {
        return Set.of(Topicos.CATALOGO);
    }

    @Override
    public void consumir(Evento evento) {
        switch (evento) {
            case ProdutoPublicado publicado -> produtos.save(new ProdutoDoEmpreendedor(
                    publicado.produtoId(), publicado.empreendedorId(), publicado.ocorridoEm()));

            case ProdutoAtualizado atualizado -> produtos.save(new ProdutoDoEmpreendedor(
                    atualizado.produtoId(), atualizado.empreendedorId(), atualizado.ocorridoEm()));

            case ProdutoRetirado retirado -> produtos.deleteById(retirado.produtoId());

            default -> {
                // Outros eventos do tópico não interessam a contagem. Ignorar
                // aqui é melhor que filtrar antes: quando um evento novo
                // aparecer, este consumidor continua funcionando sem mudança.
            }
        }
    }
}
