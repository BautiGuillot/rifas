package com.pescadoresargentinos.rifas.api.dto;

import java.util.List;

public record PremioResponse(
        Long id,
        Integer posicion,
        String descripcion,
        String imagenUrl,
        List<PremioOpcionResponse> opciones
) {
}
