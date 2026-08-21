package com.pescadoresargentinos.rifas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.AssertTrue;
import java.util.List;

public record CrearCompraRequest(
        @NotBlank String nombre,
        @NotBlank String telefono,
        @NotEmpty List<Integer> numeros,
        @AssertTrue(message = "Debes aceptar las condiciones de participación y la política de privacidad") Boolean aceptaCondiciones
) {
}
