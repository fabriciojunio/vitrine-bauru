package br.com.vitrinebauru.cadastro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * O outro lado do login: o que permite continuar logado sem manter um token
 * de acesso válido por horas.
 *
 * <h2>Guarda o resumo, nunca o token</h2>
 * A coluna é um hash SHA-256 do valor entregue ao navegador. Um vazamento do
 * banco entrega hashes, e não sessões utilizáveis. É o mesmo raciocínio da
 * senha, só que aqui basta SHA-256: o valor original tem 256 bits aleatórios,
 * então não há o que adivinhar por força bruta e não há ganho em bcrypt.
 *
 * <h2>Rotação com detecção de reuso</h2>
 * Cada renovação queima o token anterior e emite outro. Se um token já usado
 * aparecer de novo, só há duas explicações: cópia roubada ou cópia antiga em
 * uso. As duas são motivo para derrubar todas as sessões daquele usuário, que
 * é o que o caso de uso de renovação faz ao ver {@link #jaFoiUsada()}.
 */
@Entity
@Table(name = "sessao_de_renovacao", schema = "cadastro")
public class SessaoDeRenovacao {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "hash_do_token", nullable = false, unique = true, length = 64)
    private String hashDoToken;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "usada_em")
    private Instant usadaEm;

    @Column(name = "revogada_em")
    private Instant revogadaEm;

    @Column(name = "substituida_por")
    private UUID substituidaPor;

    protected SessaoDeRenovacao() {
    }

    public static SessaoDeRenovacao nova(UUID usuarioId, String hashDoToken, Instant agora, Instant expiraEm) {
        SessaoDeRenovacao sessao = new SessaoDeRenovacao();
        sessao.id = UUID.randomUUID();
        sessao.usuarioId = usuarioId;
        sessao.hashDoToken = hashDoToken;
        sessao.criadaEm = agora;
        sessao.expiraEm = expiraEm;
        return sessao;
    }

    public boolean estaValida(Instant agora) {
        return usadaEm == null && revogadaEm == null && expiraEm.isAfter(agora);
    }

    public boolean jaFoiUsada() {
        return usadaEm != null;
    }

    public void usar(UUID novaSessao, Instant agora) {
        this.usadaEm = agora;
        this.substituidaPor = novaSessao;
    }

    public void revogar(Instant agora) {
        if (revogadaEm == null) {
            this.revogadaEm = agora;
        }
    }

    public UUID id() {
        return id;
    }

    public UUID usuarioId() {
        return usuarioId;
    }

    public String hashDoToken() {
        return hashDoToken;
    }

    public Instant criadaEm() {
        return criadaEm;
    }

    public Instant expiraEm() {
        return expiraEm;
    }

    public Instant usadaEm() {
        return usadaEm;
    }

    public Instant revogadaEm() {
        return revogadaEm;
    }

    public UUID substituidaPor() {
        return substituidaPor;
    }
}
