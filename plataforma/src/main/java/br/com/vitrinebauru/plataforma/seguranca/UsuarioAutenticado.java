package br.com.vitrinebauru.plataforma.seguranca;

import java.util.UUID;

/**
 * Quem está do outro lado da requisição, já extraído do token.
 *
 * <p>Carrega o {@code empreendedorId} junto para o serviço de catálogo não
 * precisar perguntar ao cadastro quem é o dono da loja a cada produto salvo.
 * Isso é proposital: um serviço que precisa de outro no ar para responder
 * qualquer coisa não é um serviço separado, é um monolito distribuído.
 */
public record UsuarioAutenticado(UUID id, String email, Papel papel, UUID empreendedorId) {

    public boolean ehAdministrador() {
        return papel == Papel.ADMIN_SEDECON;
    }

    public boolean ehDono(UUID outroEmpreendedorId) {
        return empreendedorId != null && empreendedorId.equals(outroEmpreendedorId);
    }
}
