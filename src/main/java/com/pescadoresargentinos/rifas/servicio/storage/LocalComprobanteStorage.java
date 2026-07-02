package com.pescadoresargentinos.rifas.servicio.storage;

import java.io.IOException;
import java.net.URI;
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
@ConditionalOnProperty(name = "app.comprobantes.storage", havingValue = "local", matchIfMissing = true)
public class LocalComprobanteStorage implements ComprobanteStorage {

    private final Path comprobantesPath;

    public LocalComprobanteStorage(@Value("${app.comprobantes.path:uploads/comprobantes}") String comprobantesPath) {
        this.comprobantesPath = Path.of(comprobantesPath);
    }

    @Override
    public ComprobanteGuardado guardar(Long compraId, MultipartFile archivo) {
        try {
            Files.createDirectories(comprobantesPath);
            String nombreOriginal = nombreOriginal(archivo);
            String nombreArchivo = compraId + "-" + UUID.randomUUID() + extension(nombreOriginal);
            Path destino = comprobantesPath.resolve(nombreArchivo).normalize();
            archivo.transferTo(destino);
            return new ComprobanteGuardado(destino.toString(), nombreOriginal, archivo.getContentType());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el comprobante");
        }
    }

    @Override
    public Optional<URI> urlDescarga(String referencia, String nombreOriginal) {
        return Optional.empty();
    }

    @Override
    public Optional<ComprobanteArchivo> abrir(String referencia, String nombreOriginal, String contentType) {
        Path path = Path.of(referencia).normalize();
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(new ComprobanteArchivo(
                new FileSystemResource(path),
                nombreOriginal == null || nombreOriginal.isBlank() ? path.getFileName().toString() : nombreOriginal,
                contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType
        ));
    }

    private String nombreOriginal(MultipartFile archivo) {
        return archivo.getOriginalFilename() == null || archivo.getOriginalFilename().isBlank()
                ? "comprobante"
                : archivo.getOriginalFilename();
    }

    private String extension(String nombreOriginal) {
        int punto = nombreOriginal.lastIndexOf('.');
        return punto >= 0 ? nombreOriginal.substring(punto).toLowerCase() : "";
    }
}
