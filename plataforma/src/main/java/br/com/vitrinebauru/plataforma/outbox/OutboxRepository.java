package br.com.vitrinebauru.plataforma.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<MensagemDoOutbox, UUID> {

    /**
     * Proximas mensagens a publicar, ja travadas para esta instancia.
     *
     * <p>O {@code SKIP LOCKED} (que e o que o tempo de espera -2 liga no
     * PostgreSQL) e o que permite rodar mais de uma instancia do servico sem
     * publicar a mesma mensagem duas vezes: quem chegar depois pula as linhas
     * travadas em vez de ficar esperando, e a fila continua andando.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("""
            select m from MensagemDoOutbox m
            where m.publicadaEm is null
              and m.tentativas < :tentativasMaximas
              and (m.proximaTentativa is null or m.proximaTentativa <= :agora)
            order by m.criadaEm asc
            """)
    List<MensagemDoOutbox> proximasParaPublicar(@Param("agora") Instant agora,
                                                @Param("tentativasMaximas") int tentativasMaximas,
                                                Limit limite);

    long countByPublicadaEmIsNull();

    @Query("select count(m) from MensagemDoOutbox m where m.publicadaEm is null and m.tentativas >= :tentativasMaximas")
    long contarTravadas(@Param("tentativasMaximas") int tentativasMaximas);

    @Modifying
    @Query("delete from MensagemDoOutbox m where m.publicadaEm is not null and m.publicadaEm < :limite")
    int apagarPublicadasAntesDe(@Param("limite") Instant limite);
}
