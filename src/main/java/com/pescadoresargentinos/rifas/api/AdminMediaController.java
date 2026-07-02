package com.pescadoresargentinos.rifas.api;

import com.pescadoresargentinos.rifas.api.dto.MediaResponse;
import com.pescadoresargentinos.rifas.seguridad.UsuarioActual;
import com.pescadoresargentinos.rifas.servicio.storage.MediaGuardado;
import com.pescadoresargentinos.rifas.servicio.storage.MediaStorage;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/media")
public class AdminMediaController {

    private final MediaStorage mediaStorage;
    private final UsuarioActual usuarioActual;

    public AdminMediaController(MediaStorage mediaStorage, UsuarioActual usuarioActual) {
        this.mediaStorage = mediaStorage;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping("/premios")
    public MediaResponse subirImagenPremio(@RequestPart("archivo") MultipartFile archivo) {
        Long clienteId = usuarioActual.clienteId();
        if (clienteId == null) {
            throw new SecurityException("No tenes un cliente asociado");
        }
        MediaGuardado guardado = mediaStorage.guardarImagen("premios", clienteId, archivo);
        return new MediaResponse(guardado.url(), guardado.referencia(), guardado.nombreOriginal(), guardado.contentType());
    }
}
