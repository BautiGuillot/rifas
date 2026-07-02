package com.pescadoresargentinos.rifas.api.dto;

import java.math.BigDecimal;

public record DashboardAdminResponse(
        long rifasBorrador,
        long rifasPublicadas,
        long rifasFinalizadas,
        long rifasCanceladas,
        long comprasPendientes,
        long comprasAprobadas,
        long comprasCanceladas,
        long numerosDisponibles,
        long numerosPendientes,
        long numerosVendidos,
        BigDecimal recaudacionAprobada
) {
}
