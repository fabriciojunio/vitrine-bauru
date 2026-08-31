package br.com.vitrinebauru.cadastro.infraestrutura.persistencia;

import br.com.vitrinebauru.cadastro.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
