package br.com.vitrinebauru.cadastro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Quem fez o que, e quando.
 *
 * <p>Existe por um motivo concreto: a SEDECON aprova e suspende cadastro de
 * gente de verdade, e "quem suspendeu a minha loja?" precisa ter resposta. O
 * registro sobrevive até a exclusão de dados do titular, porque quem responde
 * pelo ato é o servidor que moderou, não o empreendedor moderado.
 */
@Entity
@Table(name = "auditoria", schema = "cadastro")
public class RegistroDeAuditoria {

    @Id
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false, length = 60)
    private String acao;

    @Column(nullable = false, length = 40)
    private String entidade;

    @Column(name = "entidade_id")
    private UUID entidadeId;

    @Column(length = 500)
    private String detalhe;

    @Column(nullable = false)
    private UUID correlacao;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    protected RegistroDeAuditoria() {
    }

    public static RegistroDeAuditoria de(UUID usuarioId, String acao, String entidade,
                                         UUID entidadeId, String detalhe,
                                         UUID correlacao, Instant agora) {
        RegistroDeAuditoria registro = new RegistroDeAuditoria();
        registro.id = UUID.randomUUID();
        registro.usuarioId = usuarioId;
        registro.acao = acao;
        registro.entidade = entidade;
        registro.entidadeId = entidadeId;
        registro.detalhe = detalhe == null ? null : detalhe.substring(0, Math.min(detalhe.length(), 500));
        registro.correlacao = correlacao;
        registro.ocorridoEm = agora;
        return registro;
    }

    public UUID id() {
        return id;
    }

    public UUID usuarioId() {
        return usuarioId;
    }

    public String acao() {
        return acao;
    }

    public String entidade() {
        return entidade;
    }

    public UUID entidadeId() {
        return entidadeId;
    }

    public String detalhe() {
        return detalhe;
    }

    public UUID correlacao() {
        return correlacao;
    }

    public Instant ocorridoEm() {
        return ocorridoEm;
    }
}
