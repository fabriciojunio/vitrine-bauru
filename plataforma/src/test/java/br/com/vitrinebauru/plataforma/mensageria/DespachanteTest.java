package br.com.vitrinebauru.plataforma.mensageria;

import br.com.vitrinebauru.contratos.CadastroAprovado;
import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.inbox.RegistroDeEntrada;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Despachante de eventos")
class DespachanteTest {

    private final MapeadorDeEventos mapeador = new MapeadorDeEventos();
    private final InboxDeMentira inbox = new InboxDeMentira();
    private final GerenteDeTransacaoDeMentira gerente = new GerenteDeTransacaoDeMentira();

    private CadastroAprovado evento;

    @BeforeEach
    void preparar() {
        evento = new CadastroAprovado(UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), "Doces da Lourdes",
                "lourdes@exemplo.com", "Lourdes");
    }

    private Despachante despachanteCom(ConsumidorDeEventos... consumidores) {
        return new Despachante(List.of(consumidores), mapeador, inbox, gerente, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("entrega so a quem assinou o topico")
    void entregaSoAQuemAssinou() {
        var deEmpreendedores = new ConsumidorDeMentira("empreendedores", Set.of(Topicos.EMPREENDEDORES));
        var deCatalogo = new ConsumidorDeMentira("catalogo", Set.of(Topicos.CATALOGO));

        despachanteCom(deEmpreendedores, deCatalogo)
                .despachar(Topicos.EMPREENDEDORES, mapeador.paraJson(evento));

        assertThat(deEmpreendedores.recebidos).hasSize(1);
        assertThat(deCatalogo.recebidos).isEmpty();
    }

    @Test
    @DisplayName("entrega o mesmo evento a dois consumidores do mesmo topico")
    void entregaAOsDois() {
        var primeiro = new ConsumidorDeMentira("primeiro", Set.of(Topicos.EMPREENDEDORES));
        var segundo = new ConsumidorDeMentira("segundo", Set.of(Topicos.EMPREENDEDORES));

        despachanteCom(primeiro, segundo)
                .despachar(Topicos.EMPREENDEDORES, mapeador.paraJson(evento));

        assertThat(primeiro.recebidos).hasSize(1);
        assertThat(segundo.recebidos).hasSize(1);
    }

    @Test
    @DisplayName("ignora a segunda entrega do mesmo evento ao mesmo consumidor")
    void ignoraRepeticao() {
        var consumidor = new ConsumidorDeMentira("um-so", Set.of(Topicos.EMPREENDEDORES));
        var despachante = despachanteCom(consumidor);
        String carga = mapeador.paraJson(evento);

        despachante.despachar(Topicos.EMPREENDEDORES, carga);
        despachante.despachar(Topicos.EMPREENDEDORES, carga);
        despachante.despachar(Topicos.EMPREENDEDORES, carga);

        assertThat(consumidor.recebidos).hasSize(1);
    }

    @Test
    @DisplayName("o mesmo evento em dois consumidores nao e repeticao")
    void doisConsumidoresNaoSaoRepeticao() {
        var primeiro = new ConsumidorDeMentira("primeiro", Set.of(Topicos.EMPREENDEDORES));
        var segundo = new ConsumidorDeMentira("segundo", Set.of(Topicos.EMPREENDEDORES));

        despachanteCom(primeiro, segundo).despachar(Topicos.EMPREENDEDORES, mapeador.paraJson(evento));

        assertThat(inbox.registros).containsExactlyInAnyOrder(
                evento.id() + "|primeiro", evento.id() + "|segundo");
    }

    @Test
    @DisplayName("deixa a falha subir para o transporte tentar de novo")
    void deixaFalhaSubir() {
        var quebrado = new ConsumidorQueQuebra();

        assertThatThrownBy(() -> despachanteCom(quebrado)
                .despachar(Topicos.EMPREENDEDORES, mapeador.paraJson(evento)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("banco fora do ar");
    }

    @Test
    @DisplayName("junta os topicos de todos os consumidores para a assinatura")
    void juntaTopicos() {
        var despachante = despachanteCom(
                new ConsumidorDeMentira("a", Set.of(Topicos.EMPREENDEDORES)),
                new ConsumidorDeMentira("b", Set.of(Topicos.CATALOGO, Topicos.CONTATOS)),
                new ConsumidorDeMentira("c", Set.of(Topicos.EMPREENDEDORES)));

        assertThat(despachante.topicosAssinados()).containsExactlyInAnyOrder(
                Topicos.EMPREENDEDORES, Topicos.CATALOGO, Topicos.CONTATOS);
    }

    @Test
    @DisplayName("servico sem consumidor nao assina nada")
    void semConsumidorNaoAssina() {
        assertThat(despachanteCom().topicosAssinados()).isEmpty();
    }

    @Test
    @DisplayName("abre uma transacao por consumidor, e nao uma para todos")
    void umaTransacaoPorConsumidor() {
        despachanteCom(
                new ConsumidorDeMentira("a", Set.of(Topicos.EMPREENDEDORES)),
                new ConsumidorDeMentira("b", Set.of(Topicos.EMPREENDEDORES)))
                .despachar(Topicos.EMPREENDEDORES, mapeador.paraJson(evento));

        assertThat(gerente.transacoesAbertas).isEqualTo(2);
    }

    private static class ConsumidorDeMentira implements ConsumidorDeEventos {
        private final String nome;
        private final Set<String> topicos;
        private final List<Evento> recebidos = new ArrayList<>();

        private ConsumidorDeMentira(String nome, Set<String> topicos) {
            this.nome = nome;
            this.topicos = topicos;
        }

        @Override
        public String nome() {
            return nome;
        }

        @Override
        public Set<String> topicos() {
            return topicos;
        }

        @Override
        public void consumir(Evento evento) {
            recebidos.add(evento);
        }
    }

    private static class ConsumidorQueQuebra implements ConsumidorDeEventos {
        @Override
        public String nome() {
            return "quebrado";
        }

        @Override
        public Set<String> topicos() {
            return Set.of(Topicos.EMPREENDEDORES);
        }

        @Override
        public void consumir(Evento evento) {
            throw new IllegalStateException("banco fora do ar");
        }
    }

    private static class InboxDeMentira implements RegistroDeEntrada {
        private final Set<String> registros = new HashSet<>();

        @Override
        public boolean registrar(Evento evento, String consumidor) {
            return registros.add(evento.id() + "|" + consumidor);
        }
    }

    private static class GerenteDeTransacaoDeMentira implements PlatformTransactionManager {
        private int transacoesAbertas;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definicao) {
            transacoesAbertas++;
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus situacao) {
        }

        @Override
        public void rollback(TransactionStatus situacao) {
        }
    }
}
