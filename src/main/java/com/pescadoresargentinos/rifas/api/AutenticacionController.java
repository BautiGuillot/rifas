package com.pescadoresargentinos.rifas.api;

import com.pescadoresargentinos.rifas.api.dto.LoginRequest;
import com.pescadoresargentinos.rifas.api.dto.LoginResponse;
import com.pescadoresargentinos.rifas.api.dto.RefreshTokenRequest;
import com.pescadoresargentinos.rifas.servicio.AutenticacionServicio;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacionController {

    private final AutenticacionServicio autenticacionServicio;

    public AutenticacionController(AutenticacionServicio autenticacionServicio) {
        this.autenticacionServicio = autenticacionServicio;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return autenticacionServicio.login(request);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return autenticacionServicio.refresh(request);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        autenticacionServicio.logout(request);
    }
}
