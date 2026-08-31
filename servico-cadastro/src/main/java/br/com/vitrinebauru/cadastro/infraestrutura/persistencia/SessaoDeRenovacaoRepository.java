package br.com.vitrinebauru.cadastro.infraestrutura.persistencia;

import br.com.vitrinebauru.cadastro.dominio.SessaoDeRenovacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessaoDeRenovacaoRepository extends JpaRepository<SessaoDeRenovacao, UUID> {

    Optional<SessaoDeRenovacao> findByHashDoToken(String hashDoToken);

    List<SessaoDeRenovacao> findByUsuarioId(UUID usuarioId);

    /** Usada quando um token já gasto reaparece: derruba tudo daquele usuário. */
    @Modifying
    @Query("""
            update SessaoDeRenovacao s
            set s.revogadaEm = :agora
            where s.usuarioId = :usuarioId and s.revogadaEm is null
            """)
    int revogarTodasDoUsuario(@Param("usuarioId") UUID usuarioId, @Param("agora") Instant agora);

    @Modifying
    @Query("delete from SessaoDeRenovacao s where s.expiraEm < :limite")
    int apagarExpiradasAntesDe(@Param("limite") Instant limite);
}
