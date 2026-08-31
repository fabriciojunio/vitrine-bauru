package br.com.vitrinebauru.plataforma.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lê o token do cabeçalho e coloca o usuário no contexto da requisição.
 *
 * <p>Token ausente ou inválido não vira erro aqui: o filtro segue adiante sem
 * autenticar e quem decide se aquele endereço exige login é a configuração de
 * segurança. Isso mantém público o que precisa ser público (a busca, a página
 * da loja) sem espalhar exceção pelo caminho normal.
 */
@Component
public class FiltroDeToken extends OncePerRequestFilter {

    private static final String CABECALHO = "Authorization";
    private static final String PREFIXO = "Bearer ";

    private final EmissorDeToken emissor;

    public FiltroDeToken(EmissorDeToken emissor) {
        this.emissor = emissor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain corrente) throws ServletException, IOException {
        String cabecalho = requisicao.getHeader(CABECALHO);

        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            emissor.ler(cabecalho.substring(PREFIXO.length())).ifPresent(usuario -> {
                var autoridades = List.of(new SimpleGrantedAuthority(usuario.papel().comoAutoridade()));
                var autenticacao = new UsernamePasswordAuthenticationToken(usuario, null, autoridades);
                autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(requisicao));
                SecurityContextHolder.getContext().setAuthentication(autenticacao);
                MDC.put("usuario", usuario.id().toString());
            });
        }

        try {
            corrente.doFilter(requisicao, resposta);
        } finally {
            MDC.remove("usuario");
            SecurityContextHolder.clearContext();
        }
    }
}
