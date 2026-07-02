package com.pescadoresargentinos.rifas.api;

import com.pescadoresargentinos.rifas.api.dto.ActualizarMarcaClienteRequest;
import com.pescadoresargentinos.rifas.api.dto.ClienteResponse;
import com.pescadoresargentinos.rifas.servicio.ClienteServicio;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/cliente")
public class AdminClienteController {

    private final ClienteServicio clienteServicio;

    public AdminClienteController(ClienteServicio clienteServicio) {
        this.clienteServicio = clienteServicio;
    }

    @GetMapping("/marca")
    public ClienteResponse obtenerMarca() {
        return clienteServicio.obtenerClienteActual();
    }

    @PutMapping("/marca")
    public ClienteResponse actualizarMarca(@Valid @RequestBody ActualizarMarcaClienteRequest request) {
        return clienteServicio.actualizarMarcaClienteActual(request);
    }

    @PostMapping("/marca/logo")
    public ClienteResponse subirLogo(@RequestPart("archivo") MultipartFile archivo) {
        return clienteServicio.subirLogoClienteActual(archivo);
    }
}
