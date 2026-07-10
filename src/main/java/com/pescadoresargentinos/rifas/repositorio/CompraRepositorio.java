package com.pescadoresargentinos.rifas.repositorio;

import com.pescadoresargentinos.rifas.dominio.Compra;
import com.pescadoresargentinos.rifas.dominio.EstadoCompra;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompraRepositorio extends JpaRepository<Compra, Long> {

    @EntityGraph(attributePaths = {"rifa", "comprador", "numeros"})
    List<Compra> findByEstadoOrderByFechaCreacionDesc(EstadoCompra estado);

    @EntityGraph(attributePaths = {"rifa", "comprador", "numeros"})
    List<Compra> findAllByOrderByFechaCreacionDesc();

    boolean existsByRifaId(Long rifaId);

    @EntityGraph(attributePaths = {"rifa", "comprador", "numeros"})
    List<Compra> findByRifaId(Long rifaId);

    List<Compra> findByRifaIdAndEstado(Long rifaId, EstadoCompra estado);

    @EntityGraph(attributePaths = {"rifa", "comprador", "numeros"})
    List<Compra> findByEstadoAndComprobanteArchivoIsNullAndComprobanteWhatsappFalseAndFechaExpiracionBefore(EstadoCompra estado, LocalDateTime fecha);

    List<Compra> findByRifaClienteIdOrderByFechaCreacionDesc(Long clienteId);

    List<Compra> findByRifaClienteIdAndEstadoOrderByFechaCreacionDesc(Long clienteId, EstadoCompra estado);

    @EntityGraph(attributePaths = {"rifa", "rifa.cliente", "comprador", "numeros"})
    Optional<Compra> findFirstByRifaClienteTwilioWhatsappFromAndCompradorTelefonoAndEstadoOrderByFechaCreacionDesc(
            String twilioWhatsappFrom,
            String telefono,
            EstadoCompra estado
    );

    long countByEstado(EstadoCompra estado);

    long countByRifaClienteIdAndEstado(Long clienteId, EstadoCompra estado);

    @Query("select coalesce(sum(c.total), 0) from Compra c where c.estado = :estado")
    BigDecimal sumarTotalPorEstado(@Param("estado") EstadoCompra estado);

    @Query("select coalesce(sum(c.total), 0) from Compra c where c.rifa.cliente.id = :clienteId and c.estado = :estado")
    BigDecimal sumarTotalPorClienteYEstado(@Param("clienteId") Long clienteId, @Param("estado") EstadoCompra estado);

    @Query("""
            select c.rifa.aliasCobro.id, count(c), coalesce(sum(c.total), 0)
            from Compra c
            where c.rifa.cliente.id = :clienteId
              and c.estado = :estado
              and c.rifa.aliasCobro is not null
            group by c.rifa.aliasCobro.id
            """)
    List<Object[]> sumarAprobadasPorAliasCobro(@Param("clienteId") Long clienteId, @Param("estado") EstadoCompra estado);

    @Query("""
            select c.rifa.id, c.estado, count(c), coalesce(sum(c.total), 0)
            from Compra c
            where c.rifa.id in :rifaIds
            group by c.rifa.id, c.estado
            """)
    List<Object[]> resumirPorRifas(@Param("rifaIds") List<Long> rifaIds);
}
