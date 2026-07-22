package com.pescadoresargentinos.rifas.api;

import com.pescadoresargentinos.rifas.api.dto.AliasCobroRequest;
import com.pescadoresargentinos.rifas.api.dto.AliasCobroDetalleResponse;
import com.pescadoresargentinos.rifas.api.dto.AliasCobroResponse;
import com.pescadoresargentinos.rifas.servicio.AliasCobroServicio;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/alias-cobro")
public class AdminAliasCobroController {

    private final AliasCobroServicio aliasCobroServicio;

    public AdminAliasCobroController(AliasCobroServicio aliasCobroServicio) {
        this.aliasCobroServicio = aliasCobroServicio;
    }

    @GetMapping
    public List<AliasCobroResponse> listar(
            @RequestParam(defaultValue = "false") boolean soloActivos,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return soloActivos
                ? aliasCobroServicio.listarActivos(desde, hasta)
                : aliasCobroServicio.listar(desde, hasta);
    }

    @GetMapping("/{id}")
    public AliasCobroDetalleResponse detalle(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return aliasCobroServicio.detalle(id, desde, hasta);
    }

    @PostMapping
    public AliasCobroResponse crear(@Valid @RequestBody AliasCobroRequest request) {
        return aliasCobroServicio.crear(request);
    }

    @PutMapping("/{id}")
    public AliasCobroResponse actualizar(@PathVariable Long id, @Valid @RequestBody AliasCobroRequest request) {
        return aliasCobroServicio.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public AliasCobroResponse cambiarEstado(@PathVariable Long id, @RequestBody EstadoAliasRequest request) {
        return aliasCobroServicio.cambiarEstado(id, request.activo());
    }

    public record EstadoAliasRequest(boolean activo) {
    }
}
