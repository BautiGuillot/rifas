package com.pescadoresargentinos.rifas.api.dto;

public record MediaResponse(
        String url,
        String referencia,
        String nombreOriginal,
        String contentType
) {
}
