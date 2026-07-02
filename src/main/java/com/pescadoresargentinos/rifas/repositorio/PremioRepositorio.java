package com.pescadoresargentinos.rifas.repositorio;

import com.pescadoresargentinos.rifas.dominio.Premio;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PremioRepositorio extends JpaRepository<Premio, Long> {

    Optional<Premio> findByRifaIdAndPosicion(Long rifaId, Integer posicion);
}
