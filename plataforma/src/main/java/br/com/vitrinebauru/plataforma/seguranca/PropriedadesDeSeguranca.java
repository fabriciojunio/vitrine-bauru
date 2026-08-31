package br.com.vitrinebauru.plataforma.seguranca;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuracao de seguranca lida do ambiente.
 *
 * <p>Nenhum valor sensivel tem padrao embutido. O segredo do token vem de
 * variavel de ambiente e o servico se recusa a subir sem ele: um segredo
 * padrao esquecido em producao e a falha que nao aparece em nenhum teste e
 * abre o sistema inteiro.
 *
 * @param segredo            chave HMAC do token, com no minimo 32 bytes
 * @param duracaoDoAcesso    validade do token de acesso
 * @param duracaoDoRefresh   validade do token de renovacao
 * @param origensPermitidas  dominios do frontend liberados no CORS
 */
@ConfigurationProperties(prefix = "vitrine.seguranca")
public record PropriedadesDeSeguranca(
        String segredo,
        Duration duracaoDoAcesso,
        Duration duracaoDoRefresh,
        List<String> origensPermitidas) {

    public PropriedadesDeSeguranca {
        if (segredo == null || segredo.getBytes().length < 32) {
            throw new IllegalStateException(
                    "vitrine.seguranca.segredo precisa ter no minimo 32 bytes. "
                            + "Defina a variavel de ambiente VITRINE_SEGREDO_JWT.");
        }
        if (duracaoDoAcesso == null) {
            duracaoDoAcesso = Duration.ofMinutes(15);
        }
        if (duracaoDoRefresh == null) {
            duracaoDoRefresh = Duration.ofDays(7);
        }
        if (duracaoDoAcesso.toMinutes() > 60) {
            throw new IllegalStateException(
                    "Token de acesso com mais de uma hora nao e aceito neste projeto: "
                            + "o que protege a sessao roubada e a validade curta mais a renovacao.");
        }
        if (origensPermitidas == null || origensPermitidas.isEmpty()) {
            origensPermitidas = List.of("http://localhost:5173");
        }
    }
}
