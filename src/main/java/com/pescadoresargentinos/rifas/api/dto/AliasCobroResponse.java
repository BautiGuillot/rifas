package com.pescadoresargentinos.rifas.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AliasCobroResponse(
        Long id,
        String nombre,
        String alias,
        String entidad,
        String titular,
        String cbuCvu,
        Boolean activo,
        LocalDateTime fechaCreacion,
        Long rifasAsociadas,
        Long rifasFinalizadas,
        Long comprasAprobadas,
        BigDecimal recaudacionAprobada
) {
}
