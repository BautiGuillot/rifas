package com.pescadoresargentinos.rifas.servicio.storage;

import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface MediaStorage {

    MediaGuardado guardarImagen(String carpeta, Long ownerId, MultipartFile archivo);

    Optional<ComprobanteArchivo> abrir(String referencia);
}
