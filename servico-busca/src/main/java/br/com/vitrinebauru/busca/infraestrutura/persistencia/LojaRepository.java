package br.com.vitrinebauru.busca.infraestrutura.persistencia;

import br.com.vitrinebauru.busca.dominio.LojaNaVitrine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LojaRepository extends JpaRepository<LojaNaVitrine, UUID> {

    Optional<LojaNaVitrine> findByApelidoNaUrlAndVisivelIsTrue(String apelidoNaUrl);

    long countByVisivelIsTrue();

    /**
     * Busca de loja por texto, bairro e categoria, com todos os filtros
     * opcionais.
     *
     * <p>O termo chega já normalizado (minúsculo e sem acento) e a coluna
     * também foi gravada assim, então a comparação é direta. Sem isso, procurar
     * "açaí" não encontraria "Açaí", que é exatamente o que o consumidor
     * digita no celular.
     */
    @Query("""
            select l from LojaNaVitrine l
            where l.visivel = true
              and (:termo is null or l.busca like %:termo%)
              and (:bairro is null or l.bairro = :bairro)
              and (:categoria is null or l.categoria = :categoria)
            order by l.nomeDoNegocio asc
            """)
    Page<LojaNaVitrine> procurar(@Param("termo") String termo,
                                 @Param("bairro") String bairro,
                                 @Param("categoria") String categoria,
                                 Pageable paginacao);

    @Query("select distinct l.bairro from LojaNaVitrine l where l.visivel = true order by l.bairro")
    List<String> bairrosComLoja();

    @Query("select distinct l.categoria from LojaNaVitrine l where l.visivel = true order by l.categoria")
    List<String> categoriasComLoja();
}
