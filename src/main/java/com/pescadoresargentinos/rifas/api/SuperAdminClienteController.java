package com.pescadoresargentinos.rifas.api;

import com.pescadoresargentinos.rifas.api.dto.ActualizarClienteRequest;
import com.pescadoresargentinos.rifas.api.dto.ActualizarEstadoClienteRequest;
import com.pescadoresargentinos.rifas.api.dto.ClienteResponse;
import com.pescadoresargentinos.rifas.api.dto.CrearClienteRequest;
import com.pescadoresargentinos.rifas.servicio.ClienteServicio;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/super-admin/clientes")
public class SuperAdminClienteController {

    private final ClienteServicio clienteServicio;

    public SuperAdminClienteController(ClienteServicio clienteServicio) {
        this.clienteServicio = clienteServicio;
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return clienteServicio.listar();
    }

    @PostMapping
    public ClienteResponse crear(@Valid @RequestBody CrearClienteRequest request) {
        return clienteServicio.crear(request);
    }

    @PutMapping("/{id}")
    public ClienteResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarClienteRequest request
    ) {
        return clienteServicio.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public ClienteResponse actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoClienteRequest request
    ) {
        return clienteServicio.actualizarEstado(id, request);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        clienteServicio.eliminar(id);
    }

    @PostMapping("/{id}/logo")
    public ClienteResponse subirLogo(
            @PathVariable Long id,
            @RequestPart("archivo") MultipartFile archivo
    ) {
        return clienteServicio.subirLogoCliente(id, archivo);
    }
}
