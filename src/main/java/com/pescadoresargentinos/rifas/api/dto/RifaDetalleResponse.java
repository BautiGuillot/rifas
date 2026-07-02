package com.pescadoresargentinos.rifas.api.dto;

import com.pescadoresargentinos.rifas.dominio.EstadoRifa;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RifaDetalleResponse(
        Long id,
        String titulo,
        String slug,
        Long clienteId,
        String clienteNombre,
        String clienteColorPrincipal,
        String clienteLogoUrl,
        String descripcion,
        Integer cantidadNumeros,
        Integer cantidadFilas,
        Integer cantidadGanadores,
        BigDecimal valorNumero,
        String aliasTransferencia,
        String whatsappComprobante,
        EstadoRifa estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaSorteo,
        List<PremioResponse> premios,
        List<NumeroResponse> numeros,
        List<GanadorResponse> ganadores
) {
}
