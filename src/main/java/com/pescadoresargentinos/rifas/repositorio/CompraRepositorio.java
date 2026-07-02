package com.pescadoresargentinos.rifas.repositorio;

import com.pescadoresargentinos.rifas.dominio.Compra;
import com.pescadoresargentinos.rifas.dominio.EstadoCompra;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    List<Compra> findByRifaIdAndEstado(Long rifaId, EstadoCompra estado);

    @EntityGraph(attributePaths = {"rifa", "comprador", "numeros"})
    List<Compra> findByEstadoAndComprobanteArchivoIsNullAndComprobanteWhatsappFalseAndFechaExpiracionBefore(EstadoCompra estado, LocalDateTime fecha);

    List<Compra> findByRifaClienteIdOrderByFechaCreacionDesc(Long clienteId);

    List<Compra> findByRifaClienteIdAndEstadoOrderByFechaCreacionDesc(Long clienteId, EstadoCompra estado);

    long countByEstado(EstadoCompra estado);

    long countByRifaClienteIdAndEstado(Long clienteId, EstadoCompra estado);

    @Query("select coalesce(sum(c.total), 0) from Compra c where c.estado = :estado")
    BigDecimal sumarTotalPorEstado(@Param("estado") EstadoCompra estado);

    @Query("select coalesce(sum(c.total), 0) from Compra c where c.rifa.cliente.id = :clienteId and c.estado = :estado")
    BigDecimal sumarTotalPorClienteYEstado(@Param("clienteId") Long clienteId, @Param("estado") EstadoCompra estado);
}
