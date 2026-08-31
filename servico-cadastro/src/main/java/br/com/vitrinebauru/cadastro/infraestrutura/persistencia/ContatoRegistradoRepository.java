package br.com.vitrinebauru.cadastro.infraestrutura.persistencia;

import br.com.vitrinebauru.cadastro.dominio.ContatoRegistrado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ContatoRegistradoRepository extends JpaRepository<ContatoRegistrado, UUID> {

    long countByOcorridoEmAfter(Instant momento);

    long countByEmpreendedorId(UUID empreendedorId);

    long countByEmpreendedorIdAndOcorridoEmAfter(UUID empreendedorId, Instant momento);

    int deleteByEmpreendedorId(UUID empreendedorId);

    /**
     * Ranking de quem mais recebe contato no período.
     *
     * <p>Devolve id e total; o nome do negócio é resolvido depois, no serviço,
     * consultando o repositório de empreendedores. Juntar as duas tabelas na
     * consulta seria mais rápido e amarraria a projeção de indicador ao
     * cadastro, que são coisas que mudam por motivos diferentes.
     */
    @Query("""
            select c.empreendedorId as empreendedorId, count(c) as total
            from ContatoRegistrado c
            where c.ocorridoEm > :desde
            group by c.empreendedorId
            order by count(c) desc
            """)
    List<TotalPorEmpreendedor> maisProcurados(@Param("desde") Instant desde);

    interface TotalPorEmpreendedor {
        UUID getEmpreendedorId();

        long getTotal();
    }
}
