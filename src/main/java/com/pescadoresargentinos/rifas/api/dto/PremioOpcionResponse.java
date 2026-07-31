package com.pescadoresargentinos.rifas.api.dto;

public record PremioOpcionResponse(
        Long id,
        Integer orden,
        String descripcion,
        String imagenUrl
) {
}
