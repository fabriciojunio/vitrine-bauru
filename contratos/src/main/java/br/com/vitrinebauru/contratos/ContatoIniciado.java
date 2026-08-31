package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/**
 * Alguem clicou para falar com o empreendedor.
 *
 * <p>E o unico numero que responde a pergunta que a SEDECON de fato faz
 * ("isso esta servindo para alguma coisa?"), porque a venda acontece fora da
 * plataforma. Visita a pagina mede curiosidade; contato iniciado mede
 * intencao.
 *
 * <p>Nada aqui identifica o consumidor: nao ha IP, nao ha cookie, nao ha
 * identificador de sessao. O evento existe para contar, nao para rastrear.
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
