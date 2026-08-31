package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/** Devolve ao ar uma loja que estava suspensa. */
public record EmpreendedorReativado(
        UUID id,
        UUID correlacao,
        Instant ocorridoEm,
        UUID empreendedorId,
        UUID reativadoPor,
        String nomeDoNegocio,
        String email,
        String nomeDoResponsavel) implements Evento {

    @Override
    public UUID chaveDeParticao() {
        return empreendedorId;
    }
}
