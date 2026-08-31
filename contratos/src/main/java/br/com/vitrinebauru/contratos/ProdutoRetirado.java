package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/** Produto saiu do catálogo e precisa sumir da busca publica. */
public record ProdutoRetirado(
        UUID id,
        UUID correlacao,
        Instant ocorridoEm,
        UUID produtoId,
        UUID empreendedorId) implements Evento {

    @Override
    public UUID chaveDeParticao() {
        return empreendedorId;
    }
}
