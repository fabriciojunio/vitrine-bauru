package br.com.vitrinebauru.notificacoes.infraestrutura.envio;

import br.com.vitrinebauru.notificacoes.dominio.Notificacao;

/**
 * Por onde o e-mail sai.
 *
 * <p>A interface existe porque o provedor é a peça mais provisória do sistema:
 * hoje é o Resend, amanhã pode ser o servidor da prefeitura. Trocar isso
 * precisa ser escrever uma classe, e não mexer em regra de negócio.
 */
public interface EnviadorDeEmail {

    void enviar(Notificacao notificacao) throws Exception;

    String descricao();
}
