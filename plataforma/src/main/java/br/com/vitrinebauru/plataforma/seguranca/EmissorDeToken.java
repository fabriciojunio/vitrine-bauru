package br.com.vitrinebauru.plataforma.seguranca;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Emite e confere o token de acesso.
 *
 * <p>O token e curto (quinze minutos por padrao) e nao pode ser revogado, que
 * e o preco de nao consultar o banco a cada requisicao. Quem revoga acesso e a
 * renovacao: o refresh fica no banco, e derrubar a sessao e apagar a linha
 * dele. Por isso a validade do acesso precisa ser curta o bastante para a
 * janela entre revogar e o token morrer nao importar.
 *
 * <p>O papel viaja dentro do token. Isso permite o catalogo autorizar sem
 * chamar o cadastro. Como o token e assinado, adulterar o papel exige o
 * segredo, que so os servicos tem.
 */
@Component
public class EmissorDeToken {

    private static final String EMISSOR = "vitrine-bauru";
    private static final String CAMPO_PAPEL = "papel";
    private static final String CAMPO_EMAIL = "email";
    private static final String CAMPO_EMPREENDEDOR = "empreendedor";

    private final SecretKey chave;
    private final PropriedadesDeSeguranca propriedades;
    private final Clock relogio;

    public EmissorDeToken(PropriedadesDeSeguranca propriedades, Clock relogio) {
        this.propriedades = propriedades;
        this.relogio = relogio;
        this.chave = Keys.hmacShaKeyFor(propriedades.segredo().getBytes(StandardCharsets.UTF_8));
    }

    public String emitir(UsuarioAutenticado usuario) {
        var agora = relogio.instant();
        var expiracao = agora.plus(propriedades.duracaoDoAcesso());

        return Jwts.builder()
                .issuer(EMISSOR)
                .subject(usuario.id().toString())
                .claim(CAMPO_EMAIL, usuario.email())
                .claim(CAMPO_PAPEL, usuario.papel().name())
                .claim(CAMPO_EMPREENDEDOR,
                        usuario.empreendedorId() == null ? null : usuario.empreendedorId().toString())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiracao))
                .signWith(chave)
                .compact();
    }

    /**
     * @return o usuario do token, ou vazio se o token for invalido, expirado,
     *         adulterado ou de outro emissor. Nao levanta excecao de proposito:
     *         token invalido e situacao esperada num endpoint publico, e nao
     *         acidente digno de pilha de erro no log.
     */
    public Optional<UsuarioAutenticado> ler(String token) {
        try {
            Claims conteudo = Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer(EMISSOR)
                    .clock(() -> Date.from(relogio.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String empreendedor = conteudo.get(CAMPO_EMPREENDEDOR, String.class);

            return Optional.of(new UsuarioAutenticado(
                    UUID.fromString(conteudo.getSubject()),
                    conteudo.get(CAMPO_EMAIL, String.class),
                    Papel.valueOf(conteudo.get(CAMPO_PAPEL, String.class)),
                    empreendedor == null ? null : UUID.fromString(empreendedor)));

        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
