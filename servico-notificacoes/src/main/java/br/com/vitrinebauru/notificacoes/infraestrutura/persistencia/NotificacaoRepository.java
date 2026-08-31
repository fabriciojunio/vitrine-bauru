package br.com.vitrinebauru.notificacoes.infraestrutura.persistencia;

import br.com.vitrinebauru.notificacoes.dominio.Notificacao;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    @Query("""
            select n from Notificacao n
            where n.enviadaEm is null
              and n.tentativas < :tentativasMaximas
              and (n.proximaTentativa is null or n.proximaTentativa <= :agora)
            order by n.criadaEm asc
            """)
    List<Notificacao> proximasParaEnviar(@Param("agora") Instant agora,
                                         @Param("tentativasMaximas") int tentativasMaximas,
                                         Limit limite);

    Page<Notificacao> findAllByOrderByCriadaEmDesc(Pageable paginacao);

    List<Notificacao> findByEmpreendedorIdOrderByCriadaEmDesc(UUID empreendedorId);

    long countByEnviadaEmIsNull();

    int deleteByEmpreendedorId(UUID empreendedorId);
}
