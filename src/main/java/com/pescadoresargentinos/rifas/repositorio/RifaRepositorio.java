package com.pescadoresargentinos.rifas.repositorio;

import com.pescadoresargentinos.rifas.dominio.EstadoRifa;
import com.pescadoresargentinos.rifas.dominio.Rifa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RifaRepositorio extends JpaRepository<Rifa, Long> {

    List<Rifa> findByEstadoOrderByIdDesc(EstadoRifa estado);

    List<Rifa> findByClienteIdOrderByIdDesc(Long clienteId);

    Optional<Rifa> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"premios", "ganadores"})
    List<Rifa> findDistinctByEstadoInOrderByIdDesc(List<EstadoRifa> estados);

    long countByEstado(EstadoRifa estado);

    long countByClienteIdAndEstado(Long clienteId, EstadoRifa estado);
}
