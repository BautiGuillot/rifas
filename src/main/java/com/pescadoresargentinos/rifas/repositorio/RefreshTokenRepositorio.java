package com.pescadoresargentinos.rifas.repositorio;

import com.pescadoresargentinos.rifas.dominio.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepositorio extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);
}
