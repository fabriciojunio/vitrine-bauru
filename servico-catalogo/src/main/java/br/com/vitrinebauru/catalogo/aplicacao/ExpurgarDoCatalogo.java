package br.com.vitrinebauru.catalogo.aplicacao;

import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.EmpreendedorConhecidoRepository;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.ImagemRepository;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.ProdutoRepository;
import br.com.vitrinebauru.contratos.ExpurgoConcluido;
import br.com.vitrinebauru.contratos.Participante;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.outbox.RegistroDeSaida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * A parte do catalogo no pedido de exclusao de dados.
 *
 * <p>Aqui e apagar de verdade: {@code delete}, e nao marcacao de retirado. A
 * diferenca importa. Retirar produto e decisao comercial, e o historico serve
 * para a auditoria; exclusao de dados e direito do titular, e o que ele pediu
 * foi que os dados sumissem.
 *
 * <p>Apagar o que ja foi apagado nao da erro e responde a mesma coisa. E essa
 * propriedade que permite o coordenador reenviar o pedido quantas vezes for
 * preciso sem estragar nada.
 */
@Component
public class ExpurgarDoCatalogo {

    private static final Logger log = LoggerFactory.getLogger(ExpurgarDoCatalogo.class);

    private final ProdutoRepository produtos;
    private final ImagemRepository imagens;
    private final EmpreendedorConhecidoRepository conhecidos;
    private final RegistroDeSaida outbox;
    private final Clock relogio;

    public ExpurgarDoCatalogo(ProdutoRepository produtos,
                              ImagemRepository imagens,
                              EmpreendedorConhecidoRepository conhecidos,
                              RegistroDeSaida outbox, Clock relogio) {
        this.produtos = produtos;
        this.imagens = imagens;
        this.conhecidos = conhecidos;
        this.outbox = outbox;
        this.relogio = relogio;
    }

    /** Executado dentro da transacao aberta pelo despachante de eventos. */
    public void executar(UUID empreendedorId, UUID correlacao) {
        int produtosRemovidos = produtos.deleteByEmpreendedorId(empreendedorId);
        int imagensRemovidas = imagens.deleteByEmpreendedorId(empreendedorId);
        conhecidos.deleteById(empreendedorId);

        int total = produtosRemovidos + imagensRemovidas;
        log.info("Expurgo do empreendedor {}: {} produtos e {} imagens apagados",
                empreendedorId, produtosRemovidos, imagensRemovidas);

        outbox.gravar(Topicos.PRIVACIDADE, new ExpurgoConcluido(
                UUID.randomUUID(), correlacao, relogio.instant(),
                empreendedorId, Participante.CATALOGO, total));
    }
}
