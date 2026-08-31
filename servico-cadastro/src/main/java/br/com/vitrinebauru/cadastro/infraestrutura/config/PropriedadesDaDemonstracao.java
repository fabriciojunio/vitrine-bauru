package br.com.vitrinebauru.cadastro.infraestrutura.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Liga o modo demonstracao.
 *
 * <p>Desligado por padrao. Isso importa: com o modo ligado existe um endereco
 * que devolve sessao valida sem senha nenhuma, e um dia esse serviço vai
 * subir com dado de gente de verdade dentro. Ligar precisa ser um ato
 * deliberado do ambiente, nunca um esquecimento.
 *
 * @param ativo  liga a entrada em um clique e a semeadura
 * @param senha  senha das contas semeadas, para quem quiser entrar pela tela normal
 */
@ConfigurationProperties(prefix = "vitrine.demo")
public record PropriedadesDaDemonstracao(boolean ativo, String senha) {

    public PropriedadesDaDemonstracao {
        if (senha == null || senha.isBlank()) {
            senha = br.com.vitrinebauru.contratos.demonstracao.DadosDaDemonstracao.SENHA_PADRAO;
        }
    }
}
