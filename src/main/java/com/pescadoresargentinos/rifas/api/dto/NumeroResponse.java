package com.pescadoresargentinos.rifas.api.dto;

import com.pescadoresargentinos.rifas.dominio.EstadoNumero;
import java.util.List;

public record NumeroResponse(
        Long id,
        Integer valor,
        String etiqueta,
        List<String> numerosIncluidos,
        EstadoNumero estado
) {
}
