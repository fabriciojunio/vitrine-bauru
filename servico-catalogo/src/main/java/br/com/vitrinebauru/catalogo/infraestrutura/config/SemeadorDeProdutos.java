package br.com.vitrinebauru.catalogo.infraestrutura.config;

import br.com.vitrinebauru.catalogo.dominio.Produto;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.CategoriaRepository;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.ProdutoRepository;
import br.com.vitrinebauru.contratos.ProdutoPublicado;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.contratos.demonstracao.DadosDaDemonstracao;
import br.com.vitrinebauru.contratos.tipos.Dinheiro;
import br.com.vitrinebauru.plataforma.outbox.RegistroDeSaida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Enche o catalogo da demonstracao.
 *
 * <p>Grava direto no repositorio, sem passar pelo caso de uso. E a unica
 * excecao a regra do projeto, e ela tem motivo: o caso de uso exige que o
 * empreendedor ja seja conhecido do catalogo, e ele so passa a ser quando o
 * evento de cadastro chega, o que ainda nao aconteceu no instante em que a
 * aplicacao esta subindo. Esperar por isso dentro de um {@code ApplicationRunner}
 * seria travar a subida do servico por causa de dado de demonstracao.
 *
 * <p>Os eventos, esses sim, sao gravados: a busca publica se enche por evento,
 * como se enche em producao.
 */
@Component
@ConditionalOnProperty(name = "vitrine.demo.ativo", havingValue = "true")
public class SemeadorDeProdutos implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SemeadorDeProdutos.class);

    private final ProdutoRepository produtos;
    private final CategoriaRepository categorias;
    private final RegistroDeSaida outbox;
    private final Clock relogio;

    public SemeadorDeProdutos(ProdutoRepository produtos,
                              CategoriaRepository categorias,
                              RegistroDeSaida outbox, Clock relogio) {
        this.produtos = produtos;
        this.categorias = categorias;
        this.outbox = outbox;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (produtos.count() > 0) {
            log.info("Catalogo da demonstracao ja semeado");
            return;
        }

        var agora = relogio.instant();
        int posicao = 0;

        for (DadosDaDemonstracao.Produto ficticio : DadosDaDemonstracao.produtos()) {
            var categoria = categorias.findByNome(ficticio.categoria()).orElseThrow(() ->
                    new IllegalStateException("Categoria da demonstracao nao existe: "
                            + ficticio.categoria()));

            // Espalha as datas para o catalogo nao parecer criado de uma vez.
            var publicadoEm = agora.minus(60L - posicao, ChronoUnit.DAYS);
            posicao++;

            var produto = produtos.save(Produto.comId(
                    ficticio.produtoId(), ficticio.empreendedorId(), ficticio.nome(),
                    ficticio.descricao(),
                    ficticio.precoEmCentavos() == null
                            ? null
                            : Dinheiro.deCentavos(ficticio.precoEmCentavos()),
                    categoria.id(), publicadoEm));

            if (!ficticio.disponivel()) {
                produto.marcarDisponivel(false, publicadoEm);
            }

            outbox.gravar(Topicos.CATALOGO, new ProdutoPublicado(
                    UUID.randomUUID(), UUID.randomUUID(), publicadoEm,
                    produto.id(), produto.empreendedorId(), produto.nome(), produto.descricao(),
                    produto.precoEmCentavos(), categoria.id(), categoria.nome(),
                    null, produto.disponivel()));
        }

        log.info("Catalogo da demonstracao semeado com {} produtos",
                DadosDaDemonstracao.produtos().size());
    }
}
