package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/**
 * Produto novo no catálogo de alguém.
 *
 * <p>O preço é opcional de propósito: "sob consulta" é uma resposta legítima
 * de quem vende bolo por encomenda ou conserta máquina de lavar, e obrigar um
 * número ali dentro empurraria o empreendedor a inventar um.
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
