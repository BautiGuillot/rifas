package com.pescadoresargentinos.rifas.api.dto;

import com.pescadoresargentinos.rifas.dominio.EstadoCompra;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompraResponse(
        Long id,
        Long rifaId,
        String rifaTitulo,
        String nombre,
        String dni,
        String telefono,
        List<String> numeros,
        BigDecimal total,
        EstadoCompra estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaExpiracion,
        String comprobanteArchivo,
        Boolean comprobanteWhatsapp,
        String twilioMensajeSid,
        String whatsappAutomaticoEstado,
        String whatsappAutomaticoError,
        LocalDateTime fechaWhatsappAutomatico,
        String aliasTransferencia,
        String whatsappComprobante
) {
}
