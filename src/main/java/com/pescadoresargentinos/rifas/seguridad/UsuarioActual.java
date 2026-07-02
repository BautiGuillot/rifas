package com.pescadoresargentinos.rifas.seguridad;

import com.pescadoresargentinos.rifas.dominio.RolUsuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class UsuarioActual {

    public String username() {
        return jwt().getSubject();
    }

    public RolUsuario rol() {
        return RolUsuario.valueOf(jwt().getClaimAsString("rol"));
    }

    public Long clienteId() {
        Number claim = jwt().getClaim("clienteId");
        return claim == null ? null : claim.longValue();
    }

    public boolean esSuperAdmin() {
        return rol() == RolUsuario.SUPER_ADMIN;
    }

    public boolean esClienteAdmin() {
        return rol() == RolUsuario.CLIENTE_ADMIN;
    }

    private Jwt jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Jwt) authentication.getPrincipal();
    }
}
