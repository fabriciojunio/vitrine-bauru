package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/**
 * Todos os participantes confirmaram e a conta foi anonimizada.
 *
 * <p>Fecha a saga e serve de comprovante: numa fiscalizacao, e este registro,
 * com data e correlacao, que mostra que o pedido foi cumprido de ponta a
 * ponta.
 */
public record ExclusaoConcluida(
        UUID id,
        UUID correlacao,
        Instant ocorridoEm,
        UUID empreendedorId,
        UUID usuarioId) implements Evento {

    @Override
    public UUID chaveDeParticao() {
        return empreendedorId;
    }
}
