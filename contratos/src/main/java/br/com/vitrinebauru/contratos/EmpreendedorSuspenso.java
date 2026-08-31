package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/** Tira a loja do ar por denuncia, golpe ou pedido do proprio empreendedor. */
public record EmpreendedorSuspenso(
        UUID id,
        UUID correlacao,
        Instant ocorridoEm,
        UUID empreendedorId,
        UUID suspensoPor,
        String motivo,
        String nomeDoNegocio,
        String email,
        String nomeDoResponsavel) implements Evento {

    @Override
    public UUID chaveDeParticao() {
        return empreendedorId;
    }
}
