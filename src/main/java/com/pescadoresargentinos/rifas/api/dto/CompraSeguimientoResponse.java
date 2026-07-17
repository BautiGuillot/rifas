package com.pescadoresargentinos.rifas.api.dto;

import com.pescadoresargentinos.rifas.dominio.EstadoCompra;

public record CompraSeguimientoResponse(
        EstadoCompra estado,
        boolean comprobanteRecibido
) {
}
