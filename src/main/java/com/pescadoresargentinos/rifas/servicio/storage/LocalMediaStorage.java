package com.pescadoresargentinos.rifas.servicio.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "app.media.storage", havingValue = "local", matchIfMissing = true)
public class LocalMediaStorage implements MediaStorage {

    private final Path mediaPath;
    private final String publicBaseUrl;

    public LocalMediaStorage(
            @Value("${app.media.path:uploads/media}") String mediaPath,
            @Value("${app.media.public-base-url:}") String publicBaseUrl
    ) {
        this.mediaPath = Path.of(mediaPath);
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public MediaGuardado guardarImagen(String carpeta, Long ownerId, MultipartFile archivo) {
        try {
            validarImagen(archivo);
            String nombreOriginal = nombreOriginal(archivo);
            String referencia = carpeta + "/" + ownerId + "/" + UUID.randomUUID() + extension(nombreOriginal);
            Path destino = mediaPath.resolve(referencia).normalize();
            Files.createDirectories(destino.getParent());
            archivo.transferTo(destino);
            return new MediaGuardado(referencia, urlPublica(referencia), nombreOriginal, archivo.getContentType());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la imagen");
        }
    }

    @Override
    public Optional<ComprobanteArchivo> abrir(String referencia) {
        Path path = mediaPath.resolve(referencia).normalize();
        if (!path.startsWith(mediaPath.normalize()) || !Files.exists(path)) {
            return Optional.empty();
        }
        String contentType = contentType(path);
        return Optional.of(new ComprobanteArchivo(new FileSystemResource(path), path.getFileName().toString(), contentType));
    }

    private String urlPublica(String referencia) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return "/api/media/" + referencia;
        }
        return publicBaseUrl.replaceAll("/$", "") + "/" + referencia;
    }

    private void validarImagen(MultipartFile archivo) {
        String contentType = archivo.getContentType() == null ? "" : archivo.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }
        if (archivo.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("La imagen no puede superar 5 MB");
        }
    }

    private String nombreOriginal(MultipartFile archivo) {
        return archivo.getOriginalFilename() == null || archivo.getOriginalFilename().isBlank()
                ? "imagen"
                : archivo.getOriginalFilename();
    }

    private String extension(String nombreOriginal) {
        int punto = nombreOriginal.lastIndexOf('.');
        return punto >= 0 ? nombreOriginal.substring(punto).toLowerCase() : "";
    }

    private String contentType(Path path) {
        try {
            String detected = Files.probeContentType(path);
            return detected == null || detected.isBlank() ? "application/octet-stream" : detected;
        } catch (IOException ex) {
            return "application/octet-stream";
        }
    }
}
