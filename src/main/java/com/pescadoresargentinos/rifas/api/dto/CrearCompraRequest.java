package com.pescadoresargentinos.rifas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CrearCompraRequest(
        @NotBlank String nombre,
        @NotBlank String dni,
        @NotBlank String telefono,
        @NotEmpty List<Integer> numeros
) {
}
