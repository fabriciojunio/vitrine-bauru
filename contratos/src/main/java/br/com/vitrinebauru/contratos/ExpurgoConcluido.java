package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/**
 * Resposta de um participante da saga: "a minha parte dos dados ja foi".
 *
 * <p>Reenviar isto e inofensivo. O coordenador guarda quem ja respondeu, entao
 * a mesma confirmacao chegando duas vezes nao adianta nem atrasa a saga.
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
