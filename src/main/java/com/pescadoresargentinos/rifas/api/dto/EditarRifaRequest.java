package com.pescadoresargentinos.rifas.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EditarRifaRequest(
        @NotBlank String titulo,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{3,80}$") String slug,
        String descripcion,
        @NotNull @Min(1) Integer cantidadNumeros,
        @NotNull @Min(1) Integer cantidadFilas,
        @NotNull @Min(1) Integer cantidadGanadores,
        @NotNull @DecimalMin("0.01") BigDecimal valorNumero,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{3,64}$") String aliasTransferencia,
        @NotBlank @Pattern(regexp = "^\\+?[1-9][0-9]{7,14}$") String whatsappComprobante,
        LocalDateTime fechaSorteo,
        @NotEmpty List<@Valid PremioRequest> premios
) {
}
