package com.pescadoresargentinos.rifas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActualizarClienteRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio") String nombre,
        @NotBlank(message = "El slug del cliente es obligatorio")
        @Pattern(
                regexp = "^[a-z0-9-]{3,80}$",
                message = "Debe tener entre 3 y 80 caracteres y usar solo letras minúsculas, números o guiones, sin espacios"
        ) String slug,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Debe ser un color hexadecimal válido") String colorPrincipal,
        String logoUrl,
        Boolean twilioWhatsappHabilitado,
        String twilioWhatsappFrom,
        String twilioMessagingServiceSid,
        String twilioContentSid,
        String whatsappConsultas,
        @NotBlank(message = "El usuario del cliente es obligatorio") String username,
        String password
) {
}
