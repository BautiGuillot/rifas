package com.pescadoresargentinos.rifas.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PremioRequest(
        @NotNull @Min(1) Integer posicion,
        @NotBlank String descripcion,
        String imagenUrl
) {
}
