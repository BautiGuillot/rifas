package com.pescadoresargentinos.rifas.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GanadorRequest(
        @NotNull @Min(1) Integer posicion,
        @NotNull @Min(0) Integer numero
) {
}
