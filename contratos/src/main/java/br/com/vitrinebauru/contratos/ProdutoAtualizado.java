package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/**
 * Estado novo e completo do produto, e nao a lista do que mudou.
 *
 * <p>Mandar o estado inteiro deixa o consumidor idempotente por construcao:
 * aplicar o mesmo evento duas vezes da no mesmo resultado. Mandar so o delta
 * exigiria que a projecao soubesse a ordem exata de chegada, garantia que o
 * broker nao da entre particoes.
 */
public record ProdutoAtualizado(
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
