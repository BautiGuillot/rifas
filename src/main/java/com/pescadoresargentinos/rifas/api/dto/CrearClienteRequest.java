package com.pescadoresargentinos.rifas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CrearClienteRequest(
        @NotBlank String nombre,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{3,80}$") String slug,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String colorPrincipal,
        String logoUrl,
        Boolean twilioWhatsappHabilitado,
        String twilioWhatsappFrom,
        String twilioMessagingServiceSid,
        String twilioContentSid,
        String whatsappConsultas,
        @NotBlank String username,
        @NotBlank String password
) {
}
