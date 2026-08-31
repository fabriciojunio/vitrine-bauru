package br.com.vitrinebauru.plataforma.seguranca;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuração de segurança lida do ambiente.
 *
 * <p>Nenhum valor sensível tem padrão embutido. O segredo do token vem de
 * variável de ambiente e o serviço se recusa a subir sem ele: um segredo
 * padrão esquecido em produção é a falha que não aparece em nenhum teste e
 * abre o sistema inteiro.
 *
 * @param segredo            chave HMAC do token, com no mínimo 32 bytes
 * @param duracaoDoAcesso    validade do token de acesso
 * @param duracaoDoRefresh   validade do token de renovação
 * @param origensPermitidas  domínios do frontend liberados no CORS
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
                    "vitrine.segurança.segredo precisa ter no mínimo 32 bytes. "
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
                    "Token de acesso com mais de uma hora não e aceito neste projeto: "
                            + "o que protege a sessão roubada e a validade curta mais a renovação.");
        }
        if (origensPermitidas == null || origensPermitidas.isEmpty()) {
            origensPermitidas = List.of("http://localhost:5173");
        }
    }
}
