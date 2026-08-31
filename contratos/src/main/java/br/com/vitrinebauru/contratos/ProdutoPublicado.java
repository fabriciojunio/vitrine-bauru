package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/**
 * Produto novo no catalogo de alguem.
 *
 * <p>O preco e opcional de proposito: "sob consulta" e uma resposta legitima
 * de quem vende bolo por encomenda ou conserta maquina de lavar, e obrigar um
 * numero ali dentro empurraria o empreendedor a inventar um.
 */
public record ProdutoPublicado(
        UUID id,
        UUID correlacao,
        Instant ocorridoEm,
        UUID produtoId,
        UUID empreendedorId,
        String nome,
        String descricao,
        Long precoEmCentavos,
        UUID categoriaId,
        String categoriaNome,
        String imagemUrl,
        boolean disponivel) implements Evento {

    @Override
    public UUID chaveDeParticao() {
        return empreendedorId;
    }
}
