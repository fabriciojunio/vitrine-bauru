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
 * Mantem, no cadastro, a contagem de produtos de cada empreendedor.
 *
 * <p>O painel da SEDECON precisa saber quem publicou produto e quem nao
 * publicou. Perguntar isso ao servico de catalogo a cada abertura do painel
 * colocaria os dois servicos no mesmo caminho critico: catalogo fora do ar,
 * painel fora do ar. Guardando por evento, o painel continua respondendo.
 *
 * <p>O preco e conhecido: entre a publicacao do produto e a chegada do evento
 * a contagem fica velha por alguns segundos. Para uma pergunta como "quem
 * ainda nao cadastrou nada", segundos de atraso nao mudam nenhuma decisao.
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
                // Outros eventos do topico nao interessam a contagem. Ignorar
                // aqui e melhor que filtrar antes: quando um evento novo
                // aparecer, este consumidor continua funcionando sem mudanca.
            }
        }
    }
}
