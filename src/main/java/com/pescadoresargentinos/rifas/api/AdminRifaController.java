package com.pescadoresargentinos.rifas.api;

import com.pescadoresargentinos.rifas.api.dto.CargarGanadoresRequest;
import com.pescadoresargentinos.rifas.api.dto.CrearRifaRequest;
import com.pescadoresargentinos.rifas.api.dto.DashboardAdminResponse;
import com.pescadoresargentinos.rifas.api.dto.EditarRifaRequest;
import com.pescadoresargentinos.rifas.api.dto.RifaDetalleResponse;
import com.pescadoresargentinos.rifas.api.dto.RifaResumenResponse;
import com.pescadoresargentinos.rifas.servicio.RifaServicio;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rifas")
public class AdminRifaController {

    private final RifaServicio rifaServicio;

    public AdminRifaController(RifaServicio rifaServicio) {
        this.rifaServicio = rifaServicio;
    }

    @GetMapping
    public List<RifaResumenResponse> listarTodas() {
        return rifaServicio.listarTodas();
    }

    @GetMapping("/dashboard")
    public DashboardAdminResponse dashboard() {
        return rifaServicio.dashboard();
    }

    @PostMapping
    public RifaDetalleResponse crear(@Valid @RequestBody CrearRifaRequest request) {
        return rifaServicio.crear(request);
    }

    @PutMapping("/{id}")
    public RifaDetalleResponse editar(@PathVariable Long id, @Valid @RequestBody EditarRifaRequest request) {
        return rifaServicio.editar(id, request);
    }

    @PatchMapping("/{id}/publicar")
    public RifaDetalleResponse publicar(@PathVariable Long id) {
        return rifaServicio.publicar(id);
    }

    @PatchMapping("/{id}/finalizar")
    public RifaDetalleResponse finalizar(@PathVariable Long id) {
        return rifaServicio.finalizar(id);
    }

    @PostMapping("/{id}/finalizar-con-ganadores")
    public RifaDetalleResponse finalizarConGanadores(
            @PathVariable Long id,
            @Valid @RequestBody CargarGanadoresRequest request
    ) {
        return rifaServicio.finalizarConGanadores(id, request);
    }

    @PatchMapping("/{id}/cancelar")
    public RifaDetalleResponse cancelar(@PathVariable Long id) {
        return rifaServicio.cancelar(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        rifaServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ganadores")
    public RifaDetalleResponse cargarGanadores(
            @PathVariable Long id,
            @Valid @RequestBody CargarGanadoresRequest request
    ) {
        return rifaServicio.cargarGanadores(id, request);
    }
}
