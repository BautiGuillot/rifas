package com.pescadoresargentinos.rifas.api.dto;

public record PremioResponse(
        Long id,
        Integer posicion,
        String descripcion,
        String imagenUrl
) {
}
