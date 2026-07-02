package com.pescadoresargentinos.rifas.api.dto;

import com.pescadoresargentinos.rifas.dominio.EstadoCliente;
import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nombre,
        String slug,
        String colorPrincipal,
        String logoUrl,
        EstadoCliente estado,
        String username,
        LocalDateTime fechaAlta
) {
}
