package com.pescadoresargentinos.rifas.servicio;

import com.pescadoresargentinos.rifas.api.dto.LoginRequest;
import com.pescadoresargentinos.rifas.api.dto.LoginResponse;
import com.pescadoresargentinos.rifas.api.dto.RefreshTokenRequest;
import com.pescadoresargentinos.rifas.dominio.RefreshToken;
import com.pescadoresargentinos.rifas.dominio.Usuario;
import com.pescadoresargentinos.rifas.repositorio.RefreshTokenRepositorio;
import com.pescadoresargentinos.rifas.repositorio.UsuarioRepositorio;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AutenticacionServicio {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepositorio refreshTokenRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final long expirationMinutes;
    private final long refreshExpirationDays;

    public AutenticacionServicio(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            RefreshTokenRepositorio refreshTokenRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes,
            @Value("${app.jwt.refresh-expiration-days}") long refreshExpirationDays
    ) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenRepositorio = refreshTokenRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.expirationMinutes = expirationMinutes;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    public LoginResponse login(LoginRequest request) {
        UserDetails user = userDetailsService.loadUserByUsername(request.username());
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Credenciales invalidas");
        }

        LoginResponse accessToken = generarAccessToken(buscarUsuario(user.getUsername()));
        RefreshToken refreshToken = crearRefreshToken(user.getUsername());
        return new LoginResponse(accessToken.token(), refreshToken.getToken(), "Bearer", accessToken.expiresAt(), accessToken.rol(), accessToken.clienteId(), accessToken.clienteNombre());
    }

    public LoginResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepositorio.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalido"));
        if (refreshToken.isRevocado() || refreshToken.getFechaExpiracion().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token invalido");
        }

        refreshToken.setRevocado(true);
        RefreshToken nuevoRefreshToken = crearRefreshToken(refreshToken.getUsername());
        LoginResponse accessToken = generarAccessToken(buscarUsuario(refreshToken.getUsername()));
        return new LoginResponse(accessToken.token(), nuevoRefreshToken.getToken(), "Bearer", accessToken.expiresAt(), accessToken.rol(), accessToken.clienteId(), accessToken.clienteNombre());
    }

    public void logout(RefreshTokenRequest request) {
        refreshTokenRepositorio.findByToken(request.refreshToken()).ifPresent(refreshToken -> {
            refreshToken.setRevocado(true);
            refreshTokenRepositorio.save(refreshToken);
        });
    }

    private LoginResponse generarAccessToken(Usuario usuario) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        Long clienteId = usuario.getCliente() == null ? null : usuario.getCliente().getId();
        String clienteNombre = usuario.getCliente() == null ? null : usuario.getCliente().getNombre();
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("rifas-backend")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(usuario.getUsername())
                .claim("roles", List.of(usuario.getRol().name()))
                .claim("rol", usuario.getRol().name());
        if (clienteId != null) {
            claimsBuilder.claim("clienteId", clienteId);
        }
        JwtClaimsSet claims = claimsBuilder.build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, null, "Bearer", expiresAt, usuario.getRol().name(), clienteId, clienteNombre);
    }

    private RefreshToken crearRefreshToken(String username) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsername(username);
        refreshToken.setToken(UUID.randomUUID().toString() + UUID.randomUUID());
        refreshToken.setFechaExpiracion(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS));
        return refreshTokenRepositorio.save(refreshToken);
    }

    private Usuario buscarUsuario(String username) {
        return usuarioRepositorio.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));
    }
}
