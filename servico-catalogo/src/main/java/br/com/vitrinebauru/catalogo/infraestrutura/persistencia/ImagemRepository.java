package br.com.vitrinebauru.catalogo.infraestrutura.persistencia;

import br.com.vitrinebauru.catalogo.dominio.ImagemDeProduto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImagemRepository extends JpaRepository<ImagemDeProduto, UUID> {

    int deleteByEmpreendedorId(UUID empreendedorId);

    @Query("select sum(i.tamanho) from ImagemDeProduto i where i.empreendedorId = :empreendedorId")
    Long espacoUsadoPor(@Param("empreendedorId") UUID empreendedorId);
}
