package com.pescadoresargentinos.rifas.api.dto;

public record GanadorResponse(
        Integer posicion,
        String numero,
        String premio,
        String nombreComprador,
        String telefonoComprador
) {
}
