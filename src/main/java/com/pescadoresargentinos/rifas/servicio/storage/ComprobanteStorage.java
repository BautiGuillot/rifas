package com.pescadoresargentinos.rifas.servicio.storage;

import java.net.URI;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface ComprobanteStorage {

    ComprobanteGuardado guardar(Long compraId, MultipartFile archivo);

    ComprobanteGuardado guardar(Long compraId, String nombreOriginal, String contentType, byte[] contenido);

    Optional<URI> urlDescarga(String referencia, String nombreOriginal);

    Optional<ComprobanteArchivo> abrir(String referencia, String nombreOriginal, String contentType);
}
