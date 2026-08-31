package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/**
 * Resposta de um participante da saga: "a minha parte dos dados já foi".
 *
 * <p>Reenviar isto é inofensivo. O coordenador guarda quem já respondeu, então
 * a mesma confirmação chegando duas vezes não adianta nem atrasa a saga.
 */
public record ExpurgoConcluido(
        UUID id,
        UUID correlacao,
        Instant ocorridoEm,
        UUID empreendedorId,
        Participante participante,
        int registrosRemovidos) implements Evento {

    @Override
    public UUID chaveDeParticao() {
        return empreendedorId;
    }
}
