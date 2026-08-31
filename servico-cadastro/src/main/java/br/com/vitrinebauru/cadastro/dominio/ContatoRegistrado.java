package br.com.vitrinebauru.cadastro.dominio;

import br.com.vitrinebauru.contratos.CanalDeContato;
import br.com.vitrinebauru.contratos.OrigemDoContato;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Um clique em "falar no WhatsApp", guardado para virar número no painel.
 *
 * <p>É a medida de impacto do projeto. A venda acontece fora da plataforma, no
 * WhatsApp, então não há pedido nem faturamento para contar; o que da para
 * medir com honestidade e quantas vezes um consumidor quis falar com um
 * empreendedor por causa da vitrine.
 *
 * <p>Não há nada aqui que identifique o consumidor. Sem IP, sem cookie, sem
 * sessão. Contar intenção não exige saber de quem ela partiu, e guardar isso
 * criaria uma base de dado pessoal que a plataforma não tem motivo para ter.
 */
@Entity
@Table(name = "contato_registrado", schema = "cadastro")
public class ContatoRegistrado {

    @Id
    private UUID id;

    @Column(name = "empreendedor_id", nullable = false)
    private UUID empreendedorId;

    @Column(name = "produto_id")
    private UUID produtoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalDeContato canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrigemDoContato origem;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    protected ContatoRegistrado() {
    }

    public ContatoRegistrado(UUID id, UUID empreendedorId, UUID produtoId,
                             CanalDeContato canal, OrigemDoContato origem, Instant ocorridoEm) {
        this.id = id;
        this.empreendedorId = empreendedorId;
        this.produtoId = produtoId;
        this.canal = canal;
        this.origem = origem;
        this.ocorridoEm = ocorridoEm;
    }

    public UUID id() {
        return id;
    }

    public UUID empreendedorId() {
        return empreendedorId;
    }

    public UUID produtoId() {
        return produtoId;
    }

    public CanalDeContato canal() {
        return canal;
    }

    public OrigemDoContato origem() {
        return origem;
    }

    public Instant ocorridoEm() {
        return ocorridoEm;
    }
}
