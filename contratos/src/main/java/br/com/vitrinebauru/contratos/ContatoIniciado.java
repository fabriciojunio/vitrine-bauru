package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/**
 * Alguém clicou para falar com o empreendedor.
 *
 * <p>É o único número que responde a pergunta que a SEDECON de fato faz
 * ("isso está servindo para alguma coisa?"), porque a venda acontece fora da
 * plataforma. Visita a página mede curiosidade; contato iniciado mede
 * intenção.
 *
 * <p>Nada aqui identifica o consumidor: não há IP, não há cookie, não há
 * identificador de sessão. O evento existe para contar, não para rastrear.
 */
public record ContatoIniciado(
        UUID id,
        UUID correlacao,
        Instant ocorridoEm,
        UUID empreendedorId,
        UUID produtoId,
        CanalDeContato canal,
        OrigemDoContato origem) implements Evento {

    @Override
    public UUID chaveDeParticao() {
        return empreendedorId;
    }
}
