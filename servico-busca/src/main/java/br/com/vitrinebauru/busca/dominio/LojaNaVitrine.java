package br.com.vitrinebauru.busca.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A loja como o consumidor a ve.
 *
 * <p>Esta tabela e uma projecao: nada aqui e escrito por requisicao de
 * usuario, tudo chega por evento do cadastro. E o lado de leitura do CQRS, e
 * existe por um motivo pratico, nao por gosto de padrao: a busca publica e o
 * unico endereco do sistema que qualquer pessoa acessa sem login, entao ela
 * precisa continuar de pe mesmo com o resto do sistema em manutencao, e
 * precisa responder rapido sem disputar banco com quem esta cadastrando.
 *
 * <p>Nao ha documento nem e-mail aqui. O consumidor precisa do nome, do bairro
 * e do WhatsApp; CPF de empreendedor numa tabela consultada sem autenticacao
 * seria vazamento esperando acontecer.
 *
 * <p>A coluna {@code visivel} e o que a moderacao controla. Loja suspensa
 * continua na tabela e sai da vitrine: quando a suspensao for revista, ela
 * volta sem precisar reconstruir nada.
 */
@Entity
@Table(name = "loja", schema = "busca")
public class LojaNaVitrine {

    @Id
    private UUID id;

    @Column(name = "nome_do_negocio", nullable = false, length = 120)
    private String nomeDoNegocio;

    @Column(name = "apelido_na_url", nullable = false, length = 60)
    private String apelidoNaUrl;

    @Column(length = 600)
    private String descricao;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(nullable = false, length = 60)
    private String bairro;

    @Column(name = "telefone_whatsapp", nullable = false, length = 11)
    private String telefoneWhatsapp;

    @Column(name = "foto_de_capa_url", length = 400)
    private String fotoDeCapaUrl;

    @Column(nullable = false)
    private boolean visivel;

    /** Texto normalizado com nome, descricao, categoria e bairro. */
    @Column(nullable = false, length = 900)
    private String busca;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    protected LojaNaVitrine() {
    }

    public static LojaNaVitrine nova(UUID id, String nomeDoNegocio, String apelidoNaUrl,
                                     String descricao, String categoria, String bairro,
                                     String telefoneWhatsapp, boolean visivel, Instant agora) {
        LojaNaVitrine loja = new LojaNaVitrine();
        loja.id = id;
        loja.atualizar(nomeDoNegocio, apelidoNaUrl, descricao, categoria, bairro,
                telefoneWhatsapp, loja.fotoDeCapaUrl, agora);
        loja.visivel = visivel;
        return loja;
    }

    public void atualizar(String nomeDoNegocio, String apelidoNaUrl, String descricao,
                          String categoria, String bairro, String telefoneWhatsapp,
                          String fotoDeCapaUrl, Instant agora) {
        this.nomeDoNegocio = nomeDoNegocio;
        this.apelidoNaUrl = apelidoNaUrl;
        this.descricao = descricao;
        this.categoria = categoria;
        this.bairro = bairro;
        this.telefoneWhatsapp = telefoneWhatsapp;
        this.fotoDeCapaUrl = fotoDeCapaUrl;
        this.atualizadaEm = agora;
        this.busca = Normalizacao.juntar(nomeDoNegocio, descricao, categoria, bairro);
    }

    public void mostrar(Instant agora) {
        this.visivel = true;
        this.atualizadaEm = agora;
    }

    public void esconder(Instant agora) {
        this.visivel = false;
        this.atualizadaEm = agora;
    }

    public UUID id() {
        return id;
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

    public String categoria() {
        return categoria;
    }

    public String bairro() {
        return bairro;
    }

    public String telefoneWhatsapp() {
        return telefoneWhatsapp;
    }

    public String fotoDeCapaUrl() {
        return fotoDeCapaUrl;
    }

    public boolean visivel() {
        return visivel;
    }

    public String busca() {
        return busca;
    }

    public Instant atualizadaEm() {
        return atualizadaEm;
    }
}
