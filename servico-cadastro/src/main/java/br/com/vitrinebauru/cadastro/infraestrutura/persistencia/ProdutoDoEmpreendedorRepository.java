package br.com.vitrinebauru.cadastro.infraestrutura.persistencia;

import br.com.vitrinebauru.cadastro.dominio.ProdutoDoEmpreendedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProdutoDoEmpreendedorRepository extends JpaRepository<ProdutoDoEmpreendedor, UUID> {

    long countByEmpreendedorId(UUID empreendedorId);

    List<ProdutoDoEmpreendedor> findByEmpreendedorId(UUID empreendedorId);

    int deleteByEmpreendedorId(UUID empreendedorId);
}
