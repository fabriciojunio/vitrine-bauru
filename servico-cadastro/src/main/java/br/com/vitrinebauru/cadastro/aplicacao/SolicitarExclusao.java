package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.dominio.PedidoDeExclusao;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.PedidoDeExclusaoRepository;
import br.com.vitrinebauru.contratos.ExclusaoSolicitada;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.outbox.RegistroDeSaida;
import br.com.vitrinebauru.plataforma.web.Correlacao;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/**
 * Abre o pedido de exclusão de dados (LGPD, artigo 18, inciso VI).
 *
 * <p>Duas coisas acontecem na hora e não esperam a saga terminar: a loja sai
 * do ar e as sessões caem. Quem pediu para sair não deve continuar aparecendo
 * na vitrine enquanto quatro serviços conversam entre si.
 *
 * <p>O apagamento de fato leva o tempo da saga. Isso é dito para o
 * empreendedor na tela, com prazo, porque prometer "seus dados foram apagados"
 * no instante do clique seria mentira: os produtos ainda estão no banco do
 * catálogo naquele segundo.
 */
@Component
public class SolicitarExclusao {

    /**
     * Prazo de resposta ao titular. Quinze dias é o que a ANPD usa como
     * referência para pedido que não pode ser atendido de imediato. Aqui a
     * saga costuma fechar em segundos; o prazo existe para o caso em que um
     * serviço fica fora do ar, e é ele que dispara o alerta.
     */
    public static final Duration PRAZO = Duration.ofDays(15);

    private final EmpreendedorRepository empreendedores;
    private final PedidoDeExclusaoRepository pedidos;
    private final EncerrarSessao encerrarSessao;
    private final RegistroDeSaida outbox;
    private final Auditor auditor;
    private final Clock relogio;

    public SolicitarExclusao(EmpreendedorRepository empreendedores, PedidoDeExclusaoRepository pedidos,
                             EncerrarSessao encerrarSessao, RegistroDeSaida outbox,
                             Auditor auditor, Clock relogio) {
        this.empreendedores = empreendedores;
        this.pedidos = pedidos;
        this.encerrarSessao = encerrarSessao;
        this.outbox = outbox;
        this.auditor = auditor;
        this.relogio = relogio;
    }

    @Transactional
    public Recibo executar(UUID empreendedorId, UUID autor) {
        var empreendedor = empreendedores.findById(empreendedorId).orElseThrow(() ->
                new ErrosDeNegocio.NaoEncontrado("Cadastro não encontrado."));

        if (pedidos.existsByEmpreendedorIdAndConcluidoEmIsNull(empreendedorId)) {
            throw new ErrosDeNegocio.Conflito(
                    "Já existe um pedido de exclusão em andamento para este cadastro.");
        }

        var agora = relogio.instant();
        var prazoLimite = agora.plus(PRAZO);

        empreendedor.marcarExcluido(agora);

        var pedido = pedidos.save(PedidoDeExclusao.novo(
                empreendedor.id(), empreendedor.usuarioId(), agora, prazoLimite));

        outbox.gravar(Topicos.PRIVACIDADE, new ExclusaoSolicitada(
                UUID.randomUUID(), Correlacao.atual(), agora,
                empreendedor.id(), empreendedor.usuarioId(), prazoLimite));

        encerrarSessao.todasDoUsuario(empreendedor.usuarioId());

        auditor.registrar(autor, "exclusao_solicitada", "empreendedor", empreendedor.id(),
                "Prazo até " + prazoLimite);

        return new Recibo(pedido.id(), agora, prazoLimite);
    }

    public record Recibo(UUID protocolo, java.time.Instant solicitadoEm, java.time.Instant prazoLimite) {
    }
}
