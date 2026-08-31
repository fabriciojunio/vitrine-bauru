package br.com.vitrinebauru.plataforma.seguranca;

import java.util.UUID;

/**
 * Quem esta do outro lado da requisicao, ja extraido do token.
 *
 * <p>Carrega o {@code empreendedorId} junto para o servico de catalogo nao
 * precisar perguntar ao cadastro quem e o dono da loja a cada produto salvo.
 * Isso e proposital: um servico que precisa de outro no ar para responder
 * qualquer coisa nao e um servico separado, e um monolito distribuido.
 */
public record UsuarioAutenticado(UUID id, String email, Papel papel, UUID empreendedorId) {

    public boolean ehAdministrador() {
        return papel == Papel.ADMIN_SEDECON;
    }

    public boolean ehDono(UUID outroEmpreendedorId) {
        return empreendedorId != null && empreendedorId.equals(outroEmpreendedorId);
    }
}
