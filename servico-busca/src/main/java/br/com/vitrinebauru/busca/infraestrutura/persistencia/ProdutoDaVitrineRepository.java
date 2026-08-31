package br.com.vitrinebauru.busca.infraestrutura.persistencia;

import br.com.vitrinebauru.busca.dominio.ProdutoNaVitrine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProdutoDaVitrineRepository extends JpaRepository<ProdutoNaVitrine, UUID> {

    List<ProdutoNaVitrine> findByEmpreendedorId(UUID empreendedorId);

    List<ProdutoNaVitrine> findByEmpreendedorIdAndVisivelIsTrueAndDisponivelIsTrue(UUID empreendedorId);

    long countByVisivelIsTrueAndDisponivelIsTrue();

    int deleteByEmpreendedorId(UUID empreendedorId);

    /**
     * A busca principal da plataforma.
     *
     * <p>Ordena por disponibilidade e depois por data: produto novo primeiro. A
     * ordenacao por relevancia de texto viria de {@code pg_trgm}, e nao entra
     * agora justamente porque a busca precisa funcionar em qualquer PostgreSQL
     * gratuito, sem extensao habilitada.
     */
    @Query("""
            select p from ProdutoNaVitrine p
            where p.visivel = true
              and p.disponivel = true
              and (:termo is null or p.busca like %:termo%)
              and (:bairro is null or p.bairro = :bairro)
              and (:categoria is null or p.categoriaNome = :categoria)
              and (:precoMaximo is null or (p.precoEmCentavos is not null
                                            and p.precoEmCentavos <= :precoMaximo))
            order by p.atualizadoEm desc
            """)
    Page<ProdutoNaVitrine> procurar(@Param("termo") String termo,
                                    @Param("bairro") String bairro,
                                    @Param("categoria") String categoria,
                                    @Param("precoMaximo") Long precoMaximo,
                                    Pageable paginacao);
}
