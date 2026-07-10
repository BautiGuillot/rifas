package com.pescadoresargentinos.rifas.repositorio;

import com.pescadoresargentinos.rifas.dominio.AliasCobro;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AliasCobroRepositorio extends JpaRepository<AliasCobro, Long> {

    List<AliasCobro> findByClienteIdOrderByActivoDescNombreAsc(Long clienteId);

    List<AliasCobro> findByClienteIdAndActivoTrueOrderByNombreAsc(Long clienteId);

    Optional<AliasCobro> findByIdAndClienteId(Long id, Long clienteId);

    Optional<AliasCobro> findFirstByClienteIdAndAliasIgnoreCase(Long clienteId, String alias);

    boolean existsByClienteIdAndAliasIgnoreCase(Long clienteId, String alias);
}
