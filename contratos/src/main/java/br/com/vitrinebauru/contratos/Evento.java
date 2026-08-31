package br.com.vitrinebauru.contratos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

/**
 * Tudo que trafega entre os servicos.
 *
 * <p>A interface e selada de proposito. O compilador passa a garantir que
 * qualquer {@code switch} sobre evento cobre todos os casos: quando alguem
 * acrescentar um evento novo, o build quebra nos lugares que precisam decidir
 * o que fazer com ele, em vez de o evento ser ignorado em silencio em
 * producao.
 *
 * <p>O par id/correlacao existe para duas perguntas diferentes. O {@code id} e
 * a identidade da mensagem e e o que torna o consumidor idempotente: se a
 * mesma mensagem chegar duas vezes, o inbox reconhece pelo id. A
 * {@code correlacao} atravessa a cadeia inteira: o cadastro que gerou a
 * aprovacao, que gerou o e-mail, que gerou a projecao, carregam a mesma
 * correlacao, e por isso o log de tres servicos diferentes pode ser lido como
 * uma historia so.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tipo")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EmpreendedorCadastrado.class, name = "EmpreendedorCadastrado"),
        @JsonSubTypes.Type(value = PerfilAtualizado.class, name = "PerfilAtualizado"),
        @JsonSubTypes.Type(value = CadastroAprovado.class, name = "CadastroAprovado"),
        @JsonSubTypes.Type(value = CadastroRejeitado.class, name = "CadastroRejeitado"),
        @JsonSubTypes.Type(value = EmpreendedorSuspenso.class, name = "EmpreendedorSuspenso"),
        @JsonSubTypes.Type(value = EmpreendedorReativado.class, name = "EmpreendedorReativado"),
        @JsonSubTypes.Type(value = ProdutoPublicado.class, name = "ProdutoPublicado"),
        @JsonSubTypes.Type(value = ProdutoAtualizado.class, name = "ProdutoAtualizado"),
        @JsonSubTypes.Type(value = ProdutoRetirado.class, name = "ProdutoRetirado"),
        @JsonSubTypes.Type(value = ContatoIniciado.class, name = "ContatoIniciado"),
        @JsonSubTypes.Type(value = ExclusaoSolicitada.class, name = "ExclusaoSolicitada"),
        @JsonSubTypes.Type(value = ExpurgoConcluido.class, name = "ExpurgoConcluido"),
        @JsonSubTypes.Type(value = ExclusaoConcluida.class, name = "ExclusaoConcluida")
})
public sealed interface Evento permits
        EmpreendedorCadastrado, PerfilAtualizado, CadastroAprovado, CadastroRejeitado,
        EmpreendedorSuspenso, EmpreendedorReativado,
        ProdutoPublicado, ProdutoAtualizado, ProdutoRetirado,
        ContatoIniciado,
        ExclusaoSolicitada, ExpurgoConcluido, ExclusaoConcluida {

    UUID id();

    UUID correlacao();

    Instant ocorridoEm();

    /**
     * Chave de particionamento no broker. Tudo que diz respeito ao mesmo
     * empreendedor cai na mesma particao, e so por isso a ordem entre
     * "aprovado" e "suspenso" e preservada. Sem isso, dois eventos do mesmo
     * empreendedor poderiam ser processados fora de ordem e a loja de alguem
     * suspenso voltaria ao ar.
     */
    UUID chaveDeParticao();

    default String tipoDoEvento() {
        return getClass().getSimpleName();
    }
}
