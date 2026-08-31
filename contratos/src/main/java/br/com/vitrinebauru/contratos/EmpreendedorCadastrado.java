package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/**
 * Um empreendedor terminou o cadastro e entrou na fila de moderação da
 * SEDECON. Ainda não aparece para ninguém: quem escuta isto guarda o perfil,
 * mas só publica quando o {@link CadastroAprovado} chegar.
 */
public record EmpreendedorCadastrado(
        UUID id,
        UUID correlacao,
        Instant ocorridoEm,
        UUID empreendedorId,
        UUID usuarioId,
        String nomeDoNegocio,
        String apelidoNaUrl,
        String descricao,
        String categoriaPrincipal,
        String bairro,
        String telefoneWhatsapp,
        String documento,
        String email,
        String nomeDoResponsavel) implements Evento {

    @Override
    public UUID chaveDeParticao() {
        return empreendedorId;
    }
}
