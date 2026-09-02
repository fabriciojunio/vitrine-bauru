package br.com.vitrinebauru.plataforma.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Uma mensagem esperando para sair.
 *
 * <p>A chave primária é o id do próprio evento, e não um id gerado aqui. Isso
 * transforma o banco em guarda contra duplicata: se a mesma regra de negócio
 * for executada duas vezes por um retry, a segunda gravação esbarra na chave
 * primária em vez de virar uma segunda mensagem no tópico.
 */
@Entity
@Table(name = "outbox")
public class MensagemDoOutbox {

    /** Depois disso a mensagem para de ser tentada e espera análise humana. */
    public static final int TENTATIVAS_MAXIMAS = 10;

    private static final Duration ESPERA_INICIAL = Duration.ofSeconds(2);
    private static final Duration ESPERA_MAXIMA = Duration.ofMinutes(10);

    @Id
    private UUID id;

    @Column(nullable = false)
    private String topico;

    @Column(nullable = false)
    private String chave;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false, columnDefinition = "text")
    private String carga;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "publicada_em")
    private Instant publicadaEm;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "proxima_tentativa")
    private Instant proximaTentativa;

    @Column(name = "ultimo_erro", length = 500)
    private String ultimoErro;

    /**
     * Contexto de rastro de quem gerou o evento, no formato do W3C.
     *
     * <p>Nulo quando o evento não nasceu de uma requisição, o que é o caso das
     * tarefas agendadas. Ver {@code RastroDaMensagem}.
     */
    @Column(name = "trace_pai", length = 80)
    private String tracePai;

    protected MensagemDoOutbox() {
    }

    public static MensagemDoOutbox nova(UUID id, String topico, String chave, String tipo,
                                        String carga, Instant agora) {
        return nova(id, topico, chave, tipo, carga, agora, null);
    }

    public static MensagemDoOutbox nova(UUID id, String topico, String chave, String tipo,
                                        String carga, Instant agora, String tracePai) {
        MensagemDoOutbox mensagem = new MensagemDoOutbox();
        mensagem.id = id;
        mensagem.topico = topico;
        mensagem.chave = chave;
        mensagem.tipo = tipo;
        mensagem.carga = carga;
        mensagem.criadaEm = agora;
        mensagem.tentativas = 0;
        mensagem.tracePai = tracePai;
        return mensagem;
    }

    public String tracePai() {
        return tracePai;
    }

    public void marcarPublicada(Instant agora) {
        this.publicadaEm = agora;
        this.ultimoErro = null;
        this.proximaTentativa = null;
    }

    /**
     * Espera dobrada a cada tentativa, até dez minutos.
     *
     * <p>Sem isso, um broker fora do ar viraria uma consulta ao banco a cada
     * meio segundo por mensagem parada, e o outbox derrubaria o banco tentando
     * consertar a falta do broker.
     */
    public void marcarFalha(String erro, Instant agora) {
        this.tentativas++;
        this.ultimoErro = erro == null ? "sem detalhe" : erro.substring(0, Math.min(erro.length(), 500));

        long segundos = Math.min(
                ESPERA_INICIAL.toSeconds() * (1L << Math.min(tentativas - 1, 10)),
                ESPERA_MAXIMA.toSeconds());
        this.proximaTentativa = agora.plusSeconds(segundos);
    }

    public boolean esgotouTentativas() {
        return tentativas >= TENTATIVAS_MAXIMAS;
    }

    public boolean foiPublicada() {
        return publicadaEm != null;
    }

    public UUID id() {
        return id;
    }

    public String topico() {
        return topico;
    }

    public String chave() {
        return chave;
    }

    public String tipo() {
        return tipo;
    }

    public String carga() {
        return carga;
    }

    public Instant criadaEm() {
        return criadaEm;
    }

    public Instant publicadaEm() {
        return publicadaEm;
    }

    public int tentativas() {
        return tentativas;
    }

    public Instant proximaTentativa() {
        return proximaTentativa;
    }

    public String ultimoErro() {
        return ultimoErro;
    }
}
