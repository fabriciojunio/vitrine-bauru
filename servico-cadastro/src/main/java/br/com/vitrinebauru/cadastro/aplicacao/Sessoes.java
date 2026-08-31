package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.dominio.SessaoDeRenovacao;
import br.com.vitrinebauru.cadastro.dominio.Usuario;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.SessaoDeRenovacaoRepository;
import br.com.vitrinebauru.plataforma.seguranca.EmissorDeToken;
import br.com.vitrinebauru.plataforma.seguranca.PropriedadesDeSeguranca;
import br.com.vitrinebauru.plataforma.seguranca.UsuarioAutenticado;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Cria e guarda sessao, para os casos de uso de login e renovacao nao
 * repetirem o mesmo par de token.
 *
 * <p>O token de renovacao e sorteado com {@link SecureRandom}, e nao derivado
 * do usuario nem de contador. Sao 256 bits sem significado nenhum: e o que
 * garante que ninguem adivinhe o token de outra pessoa a partir do proprio.
 */
@Component
public class Sessoes {

    private static final int BYTES_DO_TOKEN = 32;

    private final SessaoDeRenovacaoRepository sessoes;
    private final EmissorDeToken emissor;
    private final PropriedadesDeSeguranca propriedades;
    private final SecureRandom sorteio = new SecureRandom();
    private final Clock relogio;

    public Sessoes(SessaoDeRenovacaoRepository sessoes, EmissorDeToken emissor,
                   PropriedadesDeSeguranca propriedades, Clock relogio) {
        this.sessoes = sessoes;
        this.emissor = emissor;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    public Aberta abrir(Usuario usuario, UUID empreendedorId) {
        var agora = relogio.instant();
        var autenticado = new UsuarioAutenticado(
                usuario.id(), usuario.email(), usuario.papel(), empreendedorId);

        String tokenDeRenovacao = sortearToken();
        var sessao = sessoes.save(SessaoDeRenovacao.nova(
                usuario.id(),
                resumo(tokenDeRenovacao),
                agora,
                agora.plus(propriedades.duracaoDoRefresh())));

        return new Aberta(
                emissor.emitir(autenticado),
                tokenDeRenovacao,
                agora.plus(propriedades.duracaoDoAcesso()),
                sessao.id(),
                autenticado);
    }

    public String sortearToken() {
        byte[] bytes = new byte[BYTES_DO_TOKEN];
        sorteio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 basta aqui, e bcrypt seria desperdicio: o valor original ja e
     * aleatorio de ponta a ponta, entao nao ha dicionario que ataque isso e
     * nao ha por que tornar cada renovacao cara.
     */
    public String resumo(String token) {
        try {
            var algoritmo = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(algoritmo.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel nesta maquina virtual", e);
        }
    }

    public record Aberta(String tokenDeAcesso, String tokenDeRenovacao, Instant acessoExpiraEm,
                         UUID sessaoId, UsuarioAutenticado usuario) {
    }
}
