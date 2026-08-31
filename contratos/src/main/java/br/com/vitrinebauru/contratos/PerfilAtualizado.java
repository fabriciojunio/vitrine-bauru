package br.com.vitrinebauru.contratos;

import java.time.Instant;
import java.util.UUID;

/** O empreendedor mexeu no proprio perfil. A vitrine publica precisa refletir. */
public record PerfilAtualizado(
        UUID id,
        UUID correlacao,
        Instant ocorridoEm,
        UUID empreendedorId,
        String nomeDoNegocio,
        String apelidoNaUrl,
        String descricao,
        String categoriaPrincipal,
        String bairro,
        String telefoneWhatsapp,
        String fotoDeCapaUrl) implements Evento {

    @Override
    public UUID chaveDeParticao() {
        return empreendedorId;
    }
}
