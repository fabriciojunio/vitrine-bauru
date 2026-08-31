package br.com.vitrinebauru.cadastro.infraestrutura.persistencia;

import br.com.vitrinebauru.cadastro.dominio.Empreendedor;
import br.com.vitrinebauru.cadastro.dominio.StatusDoCadastro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmpreendedorRepository extends JpaRepository<Empreendedor, UUID> {

    Optional<Empreendedor> findByUsuarioId(UUID usuarioId);

    Optional<Empreendedor> findByApelidoNaUrl(String apelidoNaUrl);

    boolean existsByApelidoNaUrl(String apelidoNaUrl);

    boolean existsByDocumento(String documento);

    Page<Empreendedor> findByStatusOrderByCriadoEmAsc(StatusDoCadastro status, Pageable paginacao);

    long countByStatus(StatusDoCadastro status);

    /**
     * Fila de moderação ordenada do mais antigo para o mais novo.
     *
     * <p>A ordem importa mais do que parece: quem se cadastrou primeiro
     * esperou mais, e uma fila que a SEDECON atende de trás para a frente faz
     * o empreendedor mais antigo esperar para sempre.
     */
    @Query("""
            select e from Empreendedor e
            where e.status = br.com.vitrinebauru.cadastro.dominio.StatusDoCadastro.PENDENTE
            order by e.criadoEm asc
            """)
    Page<Empreendedor> filaDeModeracao(Pageable paginacao);

    @Query("""
            select e from Empreendedor e
            where e.status = br.com.vitrinebauru.cadastro.dominio.StatusDoCadastro.PENDENTE
              and e.criadoEm < :limite
            order by e.criadoEm asc
            """)
    List<Empreendedor> pendentesDesdeAntesDe(@Param("limite") Instant limite);

    @Query("select count(e) from Empreendedor e where e.status <> :status")
    long contarDiferenteDe(@Param("status") StatusDoCadastro status);
}
