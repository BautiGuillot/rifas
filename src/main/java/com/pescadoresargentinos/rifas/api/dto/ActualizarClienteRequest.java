package com.pescadoresargentinos.rifas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActualizarClienteRequest(
        @NotBlank String nombre,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{3,80}$") String slug,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String colorPrincipal,
        String logoUrl,
        @NotBlank String username,
        String password
) {
}
