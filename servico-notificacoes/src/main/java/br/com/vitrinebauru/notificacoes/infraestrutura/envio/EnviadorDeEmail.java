package br.com.vitrinebauru.notificacoes.infraestrutura.envio;

import br.com.vitrinebauru.notificacoes.dominio.Notificacao;

/**
 * Por onde o e-mail sai.
 *
 * <p>A interface existe porque o provedor e a peca mais provisoria do sistema:
 * hoje e o Resend, amanha pode ser o servidor da prefeitura. Trocar isso
 * precisa ser escrever uma classe, e nao mexer em regra de negocio.
 */
public interface EnviadorDeEmail {

    void enviar(Notificacao notificacao) throws Exception;

    String descricao();
}
