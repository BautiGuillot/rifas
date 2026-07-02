package com.pescadoresargentinos.rifas.repositorio;

import com.pescadoresargentinos.rifas.dominio.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {

    boolean existsBySlug(String slug);

    Optional<Cliente> findBySlug(String slug);
}
