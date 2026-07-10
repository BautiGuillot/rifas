package com.pescadoresargentinos.rifas.api.dto;

import com.pescadoresargentinos.rifas.dominio.EstadoRifa;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AliasCobroRifaResponse(
        Long id,
        String titulo,
        String slug,
        EstadoRifa estado,
        BigDecimal valorNumero,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaSorteo,
        Long comprasPendientes,
        Long comprasAprobadas,
        Long comprasCanceladas,
        BigDecimal recaudacionAprobada
) {
}
