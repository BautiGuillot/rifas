package com.pescadoresargentinos.rifas.api.dto;

import jakarta.validation.constraints.Pattern;

public record ActualizarMarcaClienteRequest(
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String colorPrincipal,
        String logoUrl
) {
}
