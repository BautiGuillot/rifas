package com.pescadoresargentinos.rifas.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CargarGanadoresRequest(
        @NotEmpty List<@Valid GanadorRequest> ganadores
) {
}
