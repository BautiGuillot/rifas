package com.pescadoresargentinos.rifas.api.dto;

import java.time.Instant;

public record LoginResponse(
        String token,
        String refreshToken,
        String tokenType,
        Instant expiresAt,
        String rol,
        Long clienteId,
        String clienteNombre
) {
}
