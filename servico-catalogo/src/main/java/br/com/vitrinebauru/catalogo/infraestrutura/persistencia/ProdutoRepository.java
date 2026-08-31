package br.com.vitrinebauru.catalogo.infraestrutura.persistencia;

import br.com.vitrinebauru.catalogo.dominio.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProdutoRepository extends JpaRepository<Produto, UUID> {

    Page<Produto> findByEmpreendedorIdAndRetiradoEmIsNullOrderByCriadoEmDesc(
            UUID empreendedorId, Pageable paginacao);

    List<Produto> findByEmpreendedorIdAndRetiradoEmIsNull(UUID empreendedorId);

    long countByEmpreendedorIdAndRetiradoEmIsNull(UUID empreendedorId);

    Optional<Produto> findByIdAndRetiradoEmIsNull(UUID id);

    /** Usado no expurgo de dados: apaga de verdade, e não marca como retirado. */
    int deleteByEmpreendedorId(UUID empreendedorId);

    @Query("select count(p) from Produto p where p.retiradoEm is null")
    long contarNoCatalogo();
}
