package com.pescadoresargentinos.rifas.servicio.storage;

import org.springframework.core.io.Resource;

public record ComprobanteArchivo(
        Resource resource,
        String nombreArchivo,
        String contentType
) {
}
