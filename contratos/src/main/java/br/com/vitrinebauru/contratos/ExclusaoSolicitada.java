package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/**
 * Abre a saga de exclusão de dados (LGPD, art. 18, VI).
 *
 * <p>Quem coordena é o cadastro, mas ele não pode apagar sozinho: o produto do
 * empreendedor está no catálogo, a projeção dele está na busca e o histórico
 * de e-mail está em notificações, cada um no seu banco. Este evento pede que
 * cada serviço limpe a sua parte e responda com {@link ExpurgoConcluido}.
 *
 * <p>O {@code prazoLimite} não é enfeite: a lei fala em prazo, e o coordenador
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
