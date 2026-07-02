package com.pescadoresargentinos.rifas.repositorio;

import com.pescadoresargentinos.rifas.dominio.NumeroRifa;
import com.pescadoresargentinos.rifas.dominio.EstadoNumero;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface NumeroRifaRepositorio extends JpaRepository<NumeroRifa, Long> {

    List<NumeroRifa> findByRifaIdOrderByValorAsc(Long rifaId);

    Optional<NumeroRifa> findByRifaIdAndValor(Long rifaId, Integer valor);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<NumeroRifa> findByRifaIdAndValorIn(Long rifaId, List<Integer> valores);

    long countByRifaIdAndEstado(Long rifaId, EstadoNumero estado);

    long countByEstado(EstadoNumero estado);

    long countByRifaClienteIdAndEstado(Long clienteId, EstadoNumero estado);
}
