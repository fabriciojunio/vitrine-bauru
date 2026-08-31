package br.com.vitrinebauru.cadastro.dominio;

import br.com.vitrinebauru.contratos.Participante;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * O estado da saga de exclusao de dados.
 *
 * <p>Cada servico guarda um pedaco dos dados do empreendedor no proprio
 * banco, entao apagar tudo e uma conversa entre quatro processos, nao um
 * {@code delete}. Esta linha e a memoria dessa conversa: quem ja confirmou,
 * quem falta, e ate quando da para esperar.
 *
 * <p>Nao ha compensacao possivel aqui, e isso e proposital. Uma saga de compra
 * pode estornar; exclusao de dados nao tem como desfazer, e nem deveria. O que
 * existe no lugar e reenvio ate confirmar, mais um prazo que, estourado, vira
 * alerta para uma pessoa resolver na mao. E a escolha honesta: melhor um
 * pedido de exclusao atrasado e visivel do que um pedido dado como concluido
 * com dado vivo em algum banco.
 */
@Entity
@Table(name = "pedido_de_exclusao", schema = "cadastro")
public class PedidoDeExclusao {

    @Id
    private UUID id;

    @Column(name = "empreendedor_id", nullable = false, unique = true)
    private UUID empreendedorId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "solicitado_em", nullable = false)
    private Instant solicitadoEm;

    @Column(name = "prazo_limite", nullable = false)
    private Instant prazoLimite;

    @Column(name = "concluido_em")
    private Instant concluidoEm;

    @Column(name = "ultimo_lembrete_em")
    private Instant ultimoLembreteEm;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "confirmacao_de_expurgo",
            schema = "cadastro",
            joinColumns = @JoinColumn(name = "pedido_id"))
    @Column(name = "participante", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Set<Participante> confirmados = EnumSet.noneOf(Participante.class);

    protected PedidoDeExclusao() {
    }

    public static PedidoDeExclusao novo(UUID empreendedorId, UUID usuarioId,
                                        Instant agora, Instant prazoLimite) {
        PedidoDeExclusao pedido = new PedidoDeExclusao();
        pedido.id = UUID.randomUUID();
        pedido.empreendedorId = empreendedorId;
        pedido.usuarioId = usuarioId;
        pedido.solicitadoEm = agora;
        pedido.prazoLimite = prazoLimite;
        pedido.confirmados = EnumSet.noneOf(Participante.class);
        return pedido;
    }

    /** @return {@code true} quando esta confirmacao foi a que faltava. */
    public boolean confirmar(Participante participante) {
        if (concluidoEm != null) {
            return false;
        }
        confirmados.add(participante);
        return estaCompleto();
    }

    public boolean estaCompleto() {
        return confirmados.containsAll(Participante.todos());
    }

    public Set<Participante> faltando() {
        Set<Participante> pendentes = EnumSet.copyOf(Participante.todos());
        pendentes.removeAll(confirmados);
        return pendentes;
    }

    public void concluir(Instant agora) {
        this.concluidoEm = agora;
    }

    public boolean estaAtrasado(Instant agora) {
        return concluidoEm == null && agora.isAfter(prazoLimite);
    }

    public void anotarLembrete(Instant agora) {
        this.ultimoLembreteEm = agora;
    }

    public UUID id() {
        return id;
    }

    public UUID empreendedorId() {
        return empreendedorId;
    }

    public UUID usuarioId() {
        return usuarioId;
    }

    public Instant solicitadoEm() {
        return solicitadoEm;
    }

    public Instant prazoLimite() {
        return prazoLimite;
    }

    public Instant concluidoEm() {
        return concluidoEm;
    }

    public Instant ultimoLembreteEm() {
        return ultimoLembreteEm;
    }

    public Set<Participante> confirmados() {
        return Set.copyOf(confirmados);
    }
}
