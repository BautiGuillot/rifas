package com.pescadoresargentinos.rifas.configuracion;

import com.pescadoresargentinos.rifas.dominio.RolUsuario;
import com.pescadoresargentinos.rifas.dominio.Usuario;
import com.pescadoresargentinos.rifas.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminSeeder implements CommandLineRunner {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public SuperAdminSeeder(
            UsuarioRepositorio usuarioRepositorio,
            PasswordEncoder passwordEncoder,
            @Value("${app.super-admin.username}") String username,
            @Value("${app.super-admin.password}") String password
    ) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepositorio.existsByUsername(username)) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol(RolUsuario.SUPER_ADMIN);
        usuarioRepositorio.save(usuario);
    }
}
