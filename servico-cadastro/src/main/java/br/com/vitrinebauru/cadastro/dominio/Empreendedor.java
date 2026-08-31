package br.com.vitrinebauru.cadastro.dominio;

import br.com.vitrinebauru.contratos.BairrosDeBauru;
import br.com.vitrinebauru.contratos.CategoriasDoComercio;
import br.com.vitrinebauru.contratos.tipos.ApelidoNaUrl;
import br.com.vitrinebauru.contratos.tipos.Documento;
import br.com.vitrinebauru.contratos.tipos.Telefone;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A loja: quem vende, o que vende e em que bairro.
 *
 * <p>As transições de estado ficam aqui dentro, e não no serviço de
 * aplicação, porque são a regra que não pode variar: aprovar duas vezes,
 * suspender quem nunca foi aprovado ou reativar um cadastro rejeitado são
 * erros de operação que precisam falhar do mesmo jeito venham da tela do
 * administrador, de um script de correção ou de um evento reprocessado.
 */
@Entity
@Table(name = "empreendedor", schema = "cadastro")
public class Empreendedor {

    private static final int TAMANHO_MINIMO_DO_MOTIVO = 10;

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private UUID usuarioId;

    @Column(name = "nome_do_negocio", nullable = false, length = 120)
    private String nomeDoNegocio;

    @Column(name = "apelido_na_url", nullable = false, unique = true, length = 60)
    private String apelidoNaUrl;

    @Column(length = 600)
    private String descricao;

    @Column(name = "categoria_principal", nullable = false, length = 60)
    private String categoriaPrincipal;

    @Column(nullable = false, length = 60)
    private String bairro;

    @Column(length = 8)
    private String cep;

    @Column(name = "telefone_whatsapp", nullable = false, length = 11)
    private String telefoneWhatsapp;

    @Column(nullable = false, length = 14)
    private String documento;

    @Column(name = "documento_tipo", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Documento.Tipo documentoTipo;

    @Column(name = "foto_de_capa_url", length = 400)
    private String fotoDeCapaUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDoCadastro status;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "moderado_em")
    private Instant moderadoEm;

    @Column(name = "moderado_por")
    private UUID moderadoPor;

    @Column(name = "motivo_da_moderacao", length = 400)
    private String motivoDaModeracao;

    /**
     * Resultado da consulta a base pública da Receita. Fica como texto porque
     * é informativo para quem modera, e não regra automática: reprovar
     * cadastro sozinho por causa de uma API de terceiro fora do ar seria pior
     * que deixar a decisão com a pessoa.
     */
    @Column(name = "situacao_do_documento", length = 120)
    private String situacaoDoDocumento;

    @Column(name = "documento_conferido_em")
    private Instant documentoConferidoEm;

    protected Empreendedor() {
    }

    public static Empreendedor novo(UUID usuarioId, String nomeDoNegocio, ApelidoNaUrl apelido,
                                    String descricao, String categoriaPrincipal, String bairro,
                                    String cep, Telefone telefone, Documento documento, Instant agora) {
        return comId(UUID.randomUUID(), usuarioId, nomeDoNegocio, apelido, descricao,
                categoriaPrincipal, bairro, cep, telefone, documento, agora);
    }

    /** Ver {@link Usuario#comId}: existe para a semeadura da demonstração. */
    public static Empreendedor comId(UUID id, UUID usuarioId, String nomeDoNegocio, ApelidoNaUrl apelido,
                                     String descricao, String categoriaPrincipal, String bairro,
                                     String cep, Telefone telefone, Documento documento, Instant agora) {
        Empreendedor empreendedor = new Empreendedor();
        empreendedor.id = id;
        empreendedor.usuarioId = usuarioId;
        empreendedor.nomeDoNegocio = nomeDoNegocio;
        empreendedor.apelidoNaUrl = apelido.valor();
        empreendedor.descricao = descricao;
        empreendedor.categoriaPrincipal = exigirCategoria(categoriaPrincipal);
        empreendedor.bairro = exigirBairro(bairro);
        empreendedor.cep = cep;
        empreendedor.telefoneWhatsapp = telefone.somenteDigitos();
        empreendedor.documento = documento.valor();
        empreendedor.documentoTipo = documento.tipo();
        empreendedor.status = StatusDoCadastro.PENDENTE;
        empreendedor.criadoEm = agora;
        empreendedor.atualizadoEm = agora;
        return empreendedor;
    }

    public void aprovar(UUID moderador, Instant agora) {
        exigirEstadoAtual(StatusDoCadastro.PENDENTE, "aprovar");
        this.status = StatusDoCadastro.APROVADO;
        this.motivoDaModeracao = null;
        registrarModeracao(moderador, agora);
    }

    public void rejeitar(UUID moderador, String motivo, Instant agora) {
        exigirEstadoAtual(StatusDoCadastro.PENDENTE, "rejeitar");
        this.status = StatusDoCadastro.REJEITADO;
        this.motivoDaModeracao = exigirMotivo(motivo);
        registrarModeracao(moderador, agora);
    }

    public void suspender(UUID moderador, String motivo, Instant agora) {
        exigirEstadoAtual(StatusDoCadastro.APROVADO, "suspender");
        this.status = StatusDoCadastro.SUSPENSO;
        this.motivoDaModeracao = exigirMotivo(motivo);
        registrarModeracao(moderador, agora);
    }

    public void reativar(UUID moderador, Instant agora) {
        exigirEstadoAtual(StatusDoCadastro.SUSPENSO, "reativar");
        this.status = StatusDoCadastro.APROVADO;
        this.motivoDaModeracao = null;
        registrarModeracao(moderador, agora);
    }

    /**
     * O empreendedor corrigiu o que a SEDECON apontou e volta para a fila.
     * Sem isto, uma recusa por foto ruim ou telefone errado seria definitiva,
     * e o cadastro teria que ser refeito do zero.
     */
    public void reenviarParaAnalise(Instant agora) {
        exigirEstadoAtual(StatusDoCadastro.REJEITADO, "reenviar para análise");
        this.status = StatusDoCadastro.PENDENTE;
        this.moderadoEm = null;
        this.moderadoPor = null;
        this.atualizadoEm = agora;
    }

    public void marcarExcluido(Instant agora) {
        if (status == StatusDoCadastro.EXCLUIDO) {
            throw new ErrosDeNegocio.Conflito("Este cadastro já foi excluído.");
        }
        this.status = StatusDoCadastro.EXCLUIDO;
        this.atualizadoEm = agora;
    }

    /** Tira o que identifica o negócio, mantendo a linha para a auditoria. */
    public void anonimizar(Instant agora) {
        this.nomeDoNegocio = "Cadastro removido";
        this.apelidoNaUrl = "removido-" + id;
        this.descricao = null;
        this.telefoneWhatsapp = "00000000000";
        this.documento = "00000000000";
        this.cep = null;
        this.fotoDeCapaUrl = null;
        this.situacaoDoDocumento = null;
        this.atualizadoEm = agora;
    }

    public void atualizarPerfil(String nomeDoNegocio, String descricao, String categoriaPrincipal,
                                String bairro, String cep, Telefone telefone, Instant agora) {
        if (status == StatusDoCadastro.EXCLUIDO) {
            throw new ErrosDeNegocio.Proibido("Este cadastro foi excluído e não pode ser alterado.");
        }
        this.nomeDoNegocio = nomeDoNegocio;
        this.descricao = descricao;
        this.categoriaPrincipal = exigirCategoria(categoriaPrincipal);
        this.bairro = exigirBairro(bairro);
        this.cep = cep;
        this.telefoneWhatsapp = telefone.somenteDigitos();
        this.atualizadoEm = agora;
    }

    public void trocarFotoDeCapa(String url, Instant agora) {
        this.fotoDeCapaUrl = url;
        this.atualizadoEm = agora;
    }

    public void anotarConferenciaDoDocumento(String situacao, Instant agora) {
        this.situacaoDoDocumento = situacao;
        this.documentoConferidoEm = agora;
    }

    private void registrarModeracao(UUID moderador, Instant agora) {
        this.moderadoPor = moderador;
        this.moderadoEm = agora;
        this.atualizadoEm = agora;
    }

    private void exigirEstadoAtual(StatusDoCadastro esperado, String acao) {
        if (status != esperado) {
            throw new ErrosDeNegocio.Conflito(
                    "Não dá para " + acao + " um cadastro com situação " + status.name().toLowerCase()
                            + ". A ação só vale para cadastro " + esperado.name().toLowerCase() + ".");
        }
    }

    private static String exigirMotivo(String motivo) {
        if (motivo == null || motivo.trim().length() < TAMANHO_MINIMO_DO_MOTIVO) {
            throw new ErrosDeNegocio.RegraDeNegocio(
                    "Escreva o motivo com pelo menos " + TAMANHO_MINIMO_DO_MOTIVO
                            + " caracteres. Ele vai no e-mail que o empreendedor recebe.");
        }
        return motivo.trim();
    }

    private static String exigirBairro(String bairro) {
        return BairrosDeBauru.normalizado(bairro).orElseThrow(() ->
                new ErrosDeNegocio.RegraDeNegocio(
                        "Bairro não reconhecido em Bauru. Escolha um da lista."));
    }

    private static String exigirCategoria(String categoria) {
        return CategoriasDoComercio.normalizada(categoria).orElseThrow(() ->
                new ErrosDeNegocio.RegraDeNegocio(
                        "Categoria não reconhecida. Escolha uma da lista."));
    }

    public UUID id() {
        return id;
    }

    public UUID usuarioId() {
        return usuarioId;
    }

    public String nomeDoNegocio() {
        return nomeDoNegocio;
    }

    public String apelidoNaUrl() {
        return apelidoNaUrl;
    }

    public String descricao() {
        return descricao;
    }

    public String categoriaPrincipal() {
        return categoriaPrincipal;
    }

    public String bairro() {
        return bairro;
    }

    public String cep() {
        return cep;
    }

    public String telefoneWhatsapp() {
        return telefoneWhatsapp;
    }

    public String documento() {
        return documento;
    }

    public Documento.Tipo documentoTipo() {
        return documentoTipo;
    }

    public String documentoMascarado() {
        return new Documento(documento, documentoTipo).mascarado();
    }

    public String fotoDeCapaUrl() {
        return fotoDeCapaUrl;
    }

    public StatusDoCadastro status() {
        return status;
    }

    public Instant criadoEm() {
        return criadoEm;
    }

    public Instant atualizadoEm() {
        return atualizadoEm;
    }

    public Instant moderadoEm() {
        return moderadoEm;
    }

    public UUID moderadoPor() {
        return moderadoPor;
    }

    public String motivoDaModeracao() {
        return motivoDaModeracao;
    }

    public String situacaoDoDocumento() {
        return situacaoDoDocumento;
    }

    public Instant documentoConferidoEm() {
        return documentoConferidoEm;
    }
}
