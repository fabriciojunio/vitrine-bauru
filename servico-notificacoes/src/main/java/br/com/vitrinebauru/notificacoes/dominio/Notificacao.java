package br.com.vitrinebauru.notificacoes.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Um e-mail que precisa sair.
 *
 * <p>A mensagem e gravada antes de ser enviada, e o envio acontece depois, por
 * uma tarefa que tenta de novo quando falha. E o mesmo raciocinio do outbox, e
 * pelo mesmo motivo: o servico de e-mail e de terceiro e cai. Enviar dentro do
 * consumo do evento faria a queda do provedor virar evento reprocessado em
 * laco, ou pior, cadastro aprovado sem ninguem avisado.
 *
 * <p>O corpo fica gravado junto. Ocupa espaco e vale a pena: quando o
 * empreendedor disser que nao recebeu, da para ver exatamente o que foi
 * enviado, para quem e quando.
 */
@Entity
@Table(name = "notificacao", schema = "notificacoes")
public class Notificacao {

    public static final int TENTATIVAS_MAXIMAS = 5;
    private static final Duration ESPERA_INICIAL = Duration.ofMinutes(1);
    private static final Duration ESPERA_MAXIMA = Duration.ofHours(2);

    @Id
    private UUID id;

    @Column(name = "empreendedor_id", nullable = false)
    private UUID empreendedorId;

    @Column(nullable = false, length = 160)
    private String destinatario;

    @Column(nullable = false, length = 160)
    private String assunto;

    @Column(nullable = false, columnDefinition = "text")
    private String corpo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoDeNotificacao tipo;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "enviada_em")
    private Instant enviadaEm;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "proxima_tentativa")
    private Instant proximaTentativa;

    @Column(name = "ultimo_erro", length = 400)
    private String ultimoErro;

    protected Notificacao() {
    }

    public static Notificacao nova(UUID id, UUID empreendedorId, String destinatario,
                                   TipoDeNotificacao tipo, String assunto, String corpo,
                                   Instant agora) {
        Notificacao notificacao = new Notificacao();
        notificacao.id = id;
        notificacao.empreendedorId = empreendedorId;
        notificacao.destinatario = destinatario;
        notificacao.tipo = tipo;
        notificacao.assunto = assunto;
        notificacao.corpo = corpo;
        notificacao.criadaEm = agora;
        notificacao.tentativas = 0;
        return notificacao;
    }

    public void marcarEnviada(Instant agora) {
        this.enviadaEm = agora;
        this.ultimoErro = null;
        this.proximaTentativa = null;
    }

    public void marcarFalha(String erro, Instant agora) {
        this.tentativas++;
        this.ultimoErro = erro == null ? "sem detalhe" : erro.substring(0, Math.min(erro.length(), 400));

        long minutos = Math.min(
                ESPERA_INICIAL.toMinutes() * (1L << Math.min(tentativas - 1, 7)),
                ESPERA_MAXIMA.toMinutes());
        this.proximaTentativa = agora.plusSeconds(minutos * 60);
    }

    public boolean foiEnviada() {
        return enviadaEm != null;
    }

    public boolean esgotouTentativas() {
        return tentativas >= TENTATIVAS_MAXIMAS;
    }

    public UUID id() {
        return id;
    }

    public UUID empreendedorId() {
        return empreendedorId;
    }

    public String destinatario() {
        return destinatario;
    }

    public String assunto() {
        return assunto;
    }

    public String corpo() {
        return corpo;
    }

    public TipoDeNotificacao tipo() {
        return tipo;
    }

    public Instant criadaEm() {
        return criadaEm;
    }

    public Instant enviadaEm() {
        return enviadaEm;
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
