package com.pescadoresargentinos.rifas.servicio.storage;

public record MediaGuardado(
        String referencia,
        String url,
        String nombreOriginal,
        String contentType
) {
}
