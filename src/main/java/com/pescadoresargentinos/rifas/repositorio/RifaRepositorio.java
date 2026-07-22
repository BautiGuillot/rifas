package com.pescadoresargentinos.rifas.repositorio;

import com.pescadoresargentinos.rifas.dominio.EstadoRifa;
import com.pescadoresargentinos.rifas.dominio.Rifa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RifaRepositorio extends JpaRepository<Rifa, Long> {

    List<Rifa> findByEstadoOrderByIdDesc(EstadoRifa estado);

    List<Rifa> findByClienteIdOrderByIdDesc(Long clienteId);

    Optional<Rifa> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"premios", "ganadores"})
    List<Rifa> findDistinctByEstadoInOrderByIdDesc(List<EstadoRifa> estados);

    long countByEstado(EstadoRifa estado);

    long countByClienteIdAndEstado(Long clienteId, EstadoRifa estado);

    long countByAliasCobroId(Long aliasCobroId);

    @Query("""
            select count(r)
            from Rifa r
            where r.cliente.id = :clienteId
              and (
                r.aliasCobro.id = :aliasCobroId
                or (r.aliasCobro is null and lower(r.aliasTransferencia) = lower(:alias))
              )
            """)
    long countAsignadasAAlias(
            @Param("clienteId") Long clienteId,
            @Param("aliasCobroId") Long aliasCobroId,
            @Param("alias") String alias
    );

    @Query("""
            select count(r)
            from Rifa r
            where r.cliente.id = :clienteId
              and r.estado = :estado
              and (
                r.aliasCobro.id = :aliasCobroId
                or (r.aliasCobro is null and lower(r.aliasTransferencia) = lower(:alias))
              )
            """)
    long countFinalizadasAAlias(
            @Param("clienteId") Long clienteId,
            @Param("aliasCobroId") Long aliasCobroId,
            @Param("alias") String alias,
            @Param("estado") EstadoRifa estado
    );

    @Query("""
            select count(r)
            from Rifa r
            where r.cliente.id = :clienteId
              and r.estado = :estado
              and r.fechaFinalizacion >= :desde
              and r.fechaFinalizacion < :hasta
              and (
                r.aliasCobro.id = :aliasCobroId
                or (r.aliasCobro is null and lower(r.aliasTransferencia) = lower(:alias))
              )
            """)
    long countFinalizadasAAliasEntre(
            @Param("clienteId") Long clienteId,
            @Param("aliasCobroId") Long aliasCobroId,
            @Param("alias") String alias,
            @Param("estado") EstadoRifa estado,
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta
    );

    @Query("""
            select r
            from Rifa r
            where r.cliente.id = :clienteId
              and (
                r.aliasCobro.id = :aliasCobroId
                or (r.aliasCobro is null and lower(r.aliasTransferencia) = lower(:alias))
              )
            order by r.id desc
            """)
    List<Rifa> findAsignadasAAlias(
            @Param("clienteId") Long clienteId,
            @Param("aliasCobroId") Long aliasCobroId,
            @Param("alias") String alias
    );
}
