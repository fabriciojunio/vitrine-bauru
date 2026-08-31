package br.com.vitrinebauru.cadastro.dominio;

import br.com.vitrinebauru.plataforma.seguranca.Papel;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
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
 * A conta de quem entra na plataforma.
 *
 * <p>Separada do {@link Empreendedor} porque são coisas diferentes: o
 * administrador da SEDECON tem conta e não tem loja, e a conta guarda o que
 * diz respeito a entrar (senha, bloqueio, último acesso), enquanto o
 * empreendedor guarda o que diz respeito a vender.
 */
@Entity
@Table(name = "usuario", schema = "cadastro")
public class Usuario {

    /** Depois disso a conta trava por um tempo, para força bruta não compensar. */
    public static final int TENTATIVAS_ATE_BLOQUEAR = 5;
    public static final Duration DURACAO_DO_BLOQUEIO = Duration.ofMinutes(15);

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Papel papel;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "ultimo_acesso_em")
    private Instant ultimoAcessoEm;

    @Column(name = "tentativas_falhas", nullable = false)
    private int tentativasFalhas;

    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "anonimizado_em")
    private Instant anonimizadoEm;

    protected Usuario() {
    }

    public static Usuario novo(String nome, String email, String senhaHash, Papel papel, Instant agora) {
        return comId(UUID.randomUUID(), nome, email, senhaHash, papel, agora);
    }

    /**
     * Cria a conta com um identificador escolhido.
     *
     * <p>Existe para a semeadura do modo demonstração, que precisa dos mesmos
     * identificadores nos quatro serviços. Fora dali, use {@link #novo}: id
     * vindo de fora em cadastro de verdade é caminho para colisão.
     */
    public static Usuario comId(UUID id, String nome, String email, String senhaHash,
                                Papel papel, Instant agora) {
        Usuario usuario = new Usuario();
        usuario.id = id;
        usuario.nome = nome;
        usuario.email = normalizarEmail(email);
        usuario.senhaHash = senhaHash;
        usuario.papel = papel;
        usuario.criadoEm = agora;
        usuario.ativo = true;
        usuario.tentativasFalhas = 0;
        return usuario;
    }

    /**
     * E-mail é comparado em minúsculas porque ninguém lembra se digitou
     * maiúscula no cadastro, e "não consigo entrar" por causa disso é o tipo
     * de problema que faz o empreendedor desistir da plataforma em vez de
     * abrir chamado.
     */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public boolean estaBloqueado(Instant agora) {
        return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
    }

    public void registrarAcertoDeSenha(Instant agora) {
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
        this.ultimoAcessoEm = agora;
    }

    public void registrarErroDeSenha(Instant agora) {
        this.tentativasFalhas++;
        if (tentativasFalhas >= TENTATIVAS_ATE_BLOQUEAR) {
            this.bloqueadoAte = agora.plus(DURACAO_DO_BLOQUEIO);
        }
    }

    public void trocarSenha(String novoHash) {
        this.senhaHash = novoHash;
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
    }

    /**
     * Apaga o que identifica a pessoa e mantém a linha.
     *
     * <p>Apagar a linha inteira parece mais respeitoso com o pedido, mas
     * quebraria o registro de auditoria que aponta para ela, e a auditoria de
     * quem aprovou o que precisa sobreviver ao pedido de exclusão. O caminho
     * usado aqui é o que a LGPD chama de anonimização: o dado deixa de
     * identificar alguém, e o histórico continua auditável.
     */
    public void anonimizar(Instant agora) {
        this.nome = "Conta removida";
        this.email = "removido-" + id + "@vitrinebauru.invalido";
        this.senhaHash = "conta-removida-sem-senha-valida";
        this.ativo = false;
        this.anonimizadoEm = agora;
        this.bloqueadoAte = null;
    }

    public void exigirAtiva() {
        if (!ativo) {
            throw new ErrosDeNegocio.Proibido("Esta conta está desativada.");
        }
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String email() {
        return email;
    }

    public String senhaHash() {
        return senhaHash;
    }

    public Papel papel() {
        return papel;
    }

    public Instant criadoEm() {
        return criadoEm;
    }

    public Instant ultimoAcessoEm() {
        return ultimoAcessoEm;
    }

    public int tentativasFalhas() {
        return tentativasFalhas;
    }

    public Instant bloqueadoAte() {
        return bloqueadoAte;
    }

    public boolean ativo() {
        return ativo;
    }

    public Instant anonimizadoEm() {
        return anonimizadoEm;
    }
}
