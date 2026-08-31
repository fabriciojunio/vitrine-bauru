package br.com.vitrinebauru.cadastro.infraestrutura.persistencia;

import br.com.vitrinebauru.cadastro.dominio.RegistroDeAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditoriaRepository extends JpaRepository<RegistroDeAuditoria, UUID> {

    Page<RegistroDeAuditoria> findAllByOrderByOcorridoEmDesc(Pageable paginacao);

    List<RegistroDeAuditoria> findByEntidadeIdOrderByOcorridoEmDesc(UUID entidadeId);

    List<RegistroDeAuditoria> findByUsuarioIdOrderByOcorridoEmDesc(UUID usuarioId);
}
