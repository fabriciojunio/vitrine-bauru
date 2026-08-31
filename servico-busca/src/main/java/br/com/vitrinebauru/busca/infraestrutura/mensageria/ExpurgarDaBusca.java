package br.com.vitrinebauru.busca.infraestrutura.mensageria;

import br.com.vitrinebauru.busca.infraestrutura.persistencia.LojaRepository;
import br.com.vitrinebauru.busca.infraestrutura.persistencia.ProdutoDaVitrineRepository;
import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.contratos.ExclusaoSolicitada;
import br.com.vitrinebauru.contratos.ExpurgoConcluido;
import br.com.vitrinebauru.contratos.Participante;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.mensageria.ConsumidorDeEventos;
import br.com.vitrinebauru.plataforma.outbox.RegistroDeSaida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

/**
 * A parte da busca no pedido de exclusão.
 *
 * <p>É o participante mais visível dos três: enquanto o catálogo e as
 * notificações limpam bancos internos, aqui o efeito é a loja sumir da vitrine
 * pública na hora. Por isso o expurgo apaga a linha em vez de só esconder:
 * esconder mantém nome, telefone e descrição numa tabela consultada sem login.
 */
@Component
public class ExpurgarDaBusca implements ConsumidorDeEventos {

    private static final Logger log = LoggerFactory.getLogger(ExpurgarDaBusca.class);

    private final LojaRepository lojas;
    private final ProdutoDaVitrineRepository produtos;
    private final RegistroDeSaida outbox;
    private final Clock relogio;

    public ExpurgarDaBusca(LojaRepository lojas, ProdutoDaVitrineRepository produtos,
                           RegistroDeSaida outbox, Clock relogio) {
        this.lojas = lojas;
        this.produtos = produtos;
        this.outbox = outbox;
        this.relogio = relogio;
    }

    @Override
    public String nome() {
        return "busca-expurgo";
    }

    @Override
    public Set<String> topicos() {
        return Set.of(Topicos.PRIVACIDADE);
    }

    @Override
    public void consumir(Evento evento) {
        if (!(evento instanceof ExclusaoSolicitada pedido)) {
            return;
        }

        UUID empreendedorId = pedido.empreendedorId();
        int produtosRemovidos = produtos.deleteByEmpreendedorId(empreendedorId);
        boolean tinhaLoja = lojas.findById(empreendedorId).isPresent();
        lojas.deleteById(empreendedorId);

        log.info("Expurgo na busca: loja {} e {} produtos removidos da vitrine",
                tinhaLoja ? "removida" : "já ausente", produtosRemovidos);

        outbox.gravar(Topicos.PRIVACIDADE, new ExpurgoConcluido(
                UUID.randomUUID(), pedido.correlacao(), relogio.instant(),
                empreendedorId, Participante.BUSCA, produtosRemovidos + (tinhaLoja ? 1 : 0)));
    }
}
