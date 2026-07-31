package com.pescadoresargentinos.rifas.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PremioRequest(
        @NotNull @Min(1) Integer posicion,
        String descripcion,
        String imagenUrl,
        List<@Valid PremioOpcionRequest> opciones
) {
}
