package br.com.vitrinebauru.catalogo.infraestrutura.persistencia;

import br.com.vitrinebauru.catalogo.dominio.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    List<Categoria> findAllByOrderByOrdemAsc();

    Optional<Categoria> findBySlug(String slug);

    Optional<Categoria> findByNome(String nome);
}
