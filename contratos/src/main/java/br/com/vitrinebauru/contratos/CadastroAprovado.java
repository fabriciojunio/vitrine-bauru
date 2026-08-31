package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/**
 * A SEDECON aprovou o cadastro. Este e o evento que coloca uma loja no ar e
 * dispara o e-mail de boas-vindas.
 */
public record CadastroAprovado(
        UUID id,
        UUID correlacao,
        Instant ocorridoEm,
        UUID empreendedorId,
        UUID aprovadoPor,
        String nomeDoNegocio,
        String email,
        String nomeDoResponsavel) implements Evento {

    @Override
    public UUID chaveDeParticao() {
        return empreendedorId;
    }
}
