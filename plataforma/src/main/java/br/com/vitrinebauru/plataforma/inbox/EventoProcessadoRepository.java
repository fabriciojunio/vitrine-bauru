package br.com.vitrinebauru.plataforma.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, EventoProcessado.Chave> {

    @Modifying
    @Query("delete from EventoProcessado e where e.processadoEm < :limite")
    int apagarAnterioresA(@Param("limite") Instant limite);
}
