package com.pescadoresargentinos.rifas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record CrearCompraRequest(
        @NotBlank String nombre,
        @NotBlank String dni,
        @NotBlank @Pattern(regexp = "^\\+?[1-9][0-9]{7,14}$") String telefono,
        @NotEmpty List<Integer> numeros
) {
}
