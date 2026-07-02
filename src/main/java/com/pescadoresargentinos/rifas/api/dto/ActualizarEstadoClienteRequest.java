package com.pescadoresargentinos.rifas.api.dto;

import com.pescadoresargentinos.rifas.dominio.EstadoCliente;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoClienteRequest(
        @NotNull EstadoCliente estado
) {
}
