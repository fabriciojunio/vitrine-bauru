package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/**
 * A SEDECON recusou o cadastro. O motivo vai junto porque ele e escrito pelo
 * analista e precisa chegar inteiro no e-mail do empreendedor: recusar sem
 * dizer por que e o jeito mais rapido de perder a confianca de quem estava
 * disposto a se cadastrar.
 */
public record CadastroRejeitado(
        UUID id,
        UUID correlacao,
        Instant ocorridoEm,
        UUID empreendedorId,
        UUID rejeitadoPor,
        String motivo,
        String nomeDoNegocio,
        String email,
        String nomeDoResponsavel) implements Evento {

    @Override
    public UUID chaveDeParticao() {
        return empreendedorId;
    }
}
