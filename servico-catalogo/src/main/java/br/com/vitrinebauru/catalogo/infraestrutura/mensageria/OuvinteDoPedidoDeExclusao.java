package br.com.vitrinebauru.catalogo.infraestrutura.mensageria;

import br.com.vitrinebauru.catalogo.aplicacao.ExpurgarDoCatalogo;
import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.contratos.ExclusaoSolicitada;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.mensageria.ConsumidorDeEventos;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Recebe o pedido de exclusão e aciona o expurgo do catálogo. */
@Component
public class OuvinteDoPedidoDeExclusao implements ConsumidorDeEventos {

    private final ExpurgarDoCatalogo expurgar;

    public OuvinteDoPedidoDeExclusao(ExpurgarDoCatalogo expurgar) {
        this.expurgar = expurgar;
    }

    @Override
    public String nome() {
        return "catalogo-expurgo";
    }

    @Override
    public Set<String> topicos() {
        return Set.of(Topicos.PRIVACIDADE);
    }

    @Override
    public void consumir(Evento evento) {
        if (evento instanceof ExclusaoSolicitada pedido) {
            expurgar.executar(pedido.empreendedorId(), pedido.correlacao());
        }
    }
}
