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
        String aclaracionSorteo,
        Integer cantidadNumeros,
        Integer cantidadFilas,
        Integer cantidadGanadores,
        BigDecimal valorNumero,
        Long aliasCobroId,
        String aliasCobroNombre,
        String aliasCobroEntidad,
        String aliasCobroTitular,
        String aliasCobroCbuCvu,
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
