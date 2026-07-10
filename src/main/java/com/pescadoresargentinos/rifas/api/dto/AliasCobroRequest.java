package com.pescadoresargentinos.rifas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AliasCobroRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{3,64}$") String alias,
        @Size(max = 120) String entidad,
        @Size(max = 120) String titular,
        @Size(max = 32) String cbuCvu,
        Boolean activo
) {
}
