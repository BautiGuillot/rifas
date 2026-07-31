package com.pescadoresargentinos.rifas.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PremioOpcionRequest(
        @NotBlank String descripcion,
        String imagenUrl
) {
}
