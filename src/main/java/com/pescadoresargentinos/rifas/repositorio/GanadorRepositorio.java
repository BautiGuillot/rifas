package com.pescadoresargentinos.rifas.repositorio;

import com.pescadoresargentinos.rifas.dominio.Ganador;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GanadorRepositorio extends JpaRepository<Ganador, Long> {

    List<Ganador> findByRifaIdOrderByPosicionAsc(Long rifaId);

    void deleteByRifaId(Long rifaId);
}
