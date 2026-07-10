package com.pescadoresargentinos.rifas.api.dto;

import java.util.List;

public record AliasCobroDetalleResponse(
        AliasCobroResponse alias,
        List<AliasCobroRifaResponse> rifas
) {
}
