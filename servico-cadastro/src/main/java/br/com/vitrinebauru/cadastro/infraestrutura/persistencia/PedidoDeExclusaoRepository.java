package br.com.vitrinebauru.cadastro.infraestrutura.persistencia;

import br.com.vitrinebauru.cadastro.dominio.PedidoDeExclusao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PedidoDeExclusaoRepository extends JpaRepository<PedidoDeExclusao, UUID> {

    Optional<PedidoDeExclusao> findByEmpreendedorId(UUID empreendedorId);

    boolean existsByEmpreendedorIdAndConcluidoEmIsNull(UUID empreendedorId);

    @Query("select p from PedidoDeExclusao p where p.concluidoEm is null")
    List<PedidoDeExclusao> emAndamento();

    @Query("select p from PedidoDeExclusao p where p.concluidoEm is null and p.prazoLimite < :agora")
    List<PedidoDeExclusao> atrasados(@Param("agora") Instant agora);
}
