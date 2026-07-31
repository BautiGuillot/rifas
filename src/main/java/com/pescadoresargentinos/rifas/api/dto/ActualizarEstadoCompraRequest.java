package com.pescadoresargentinos.rifas.api.dto;

import com.pescadoresargentinos.rifas.dominio.EstadoCompra;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoCompraRequest(
        @NotNull EstadoCompra estado
) {
}
