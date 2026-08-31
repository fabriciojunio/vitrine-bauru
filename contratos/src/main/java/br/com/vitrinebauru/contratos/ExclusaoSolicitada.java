package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/**
 * Abre a saga de exclusao de dados (LGPD, art. 18, VI).
 *
 * <p>Quem coordena e o cadastro, mas ele nao pode apagar sozinho: o produto do
 * empreendedor esta no catalogo, a projecao dele esta na busca e o historico
 * de e-mail esta em notificacoes, cada um no seu banco. Este evento pede que
 * cada servico limpe a sua parte e responda com {@link ExpurgoConcluido}.
 *
 * <p>O {@code prazoLimite} nao e enfeite: a lei fala em prazo, e o coordenador
 * usa essa data para saber quando parar de esperar e escalar para uma pessoa.
 */
public record ExclusaoSolicitada(
        UUID id,
        UUID correlacao,
        Instant ocorridoEm,
        UUID empreendedorId,
        UUID usuarioId,
        Instant prazoLimite) implements Evento {

    @Override
    public UUID chaveDeParticao() {
        return empreendedorId;
    }
}
