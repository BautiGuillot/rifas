package com.pescadoresargentinos.rifas.api;

import com.pescadoresargentinos.rifas.servicio.storage.ComprobanteArchivo;
import com.pescadoresargentinos.rifas.servicio.storage.MediaStorage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaStorage mediaStorage;

    public MediaController(MediaStorage mediaStorage) {
        this.mediaStorage = mediaStorage;
    }

    @GetMapping("/**")
    public ResponseEntity<?> abrir(HttpServletRequest request) {
        String prefix = "/api/media/";
        String referencia = request.getRequestURI().substring(request.getRequestURI().indexOf(prefix) + prefix.length());
        ComprobanteArchivo archivo = mediaStorage.abrir(referencia)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la imagen"));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(archivo.nombreArchivo()).build().toString())
                .header(HttpHeaders.CONTENT_TYPE, archivo.contentType())
                .body(archivo.resource());
    }
}
