package br.com.vitrinebauru.notificacoes.infraestrutura.envio;

import br.com.vitrinebauru.notificacoes.dominio.Notificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * O envio de desenvolvimento e da demonstracao: escreve no log em vez de
 * mandar e-mail.
 *
 * <p>Existe para que ninguem precise de chave de provedor para rodar o
 * projeto, e para que a demonstracao publica nao dispare e-mail de verdade
 * para os enderecos ficticios das lojas semeadas. E o padrao: ligar o envio
 * de verdade exige dizer {@code vitrine.email.ativo=true} no ambiente.
 *
 * <p>Registra o destinatario e o assunto, nunca o corpo inteiro. Corpo de
 * e-mail transacional carrega nome e motivo de recusa, e log e o lugar onde
 * dado pessoal costuma vazar sem ninguem perceber.
 */
@Component
@ConditionalOnProperty(name = "vitrine.email.ativo", havingValue = "false", matchIfMissing = true)
public class EnviadorParaOLog implements EnviadorDeEmail {

    private static final Logger log = LoggerFactory.getLogger(EnviadorParaOLog.class);

    @Override
    public void enviar(Notificacao notificacao) {
        log.info("E-mail nao enviado de verdade (sem provedor configurado): {} para {} [{}]",
                notificacao.tipo(), mascarar(notificacao.destinatario()), notificacao.assunto());
    }

    private String mascarar(String email) {
        int arroba = email.indexOf('@');
        if (arroba <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(arroba);
    }

    @Override
    public String descricao() {
        return "log";
    }
}
