package com.pescadoresargentinos.rifas.servicio.storage;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.web.multipart.MultipartFile;

public final class ArchivoSeguro {

    public static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
    private static final String JPEG = "image/jpeg";
    private static final String PNG = "image/png";
    private static final String WEBP = "image/webp";
    private static final String PDF = "application/pdf";
    private static final Pattern REFERENCIA_MEDIA_PUBLICA = Pattern.compile(
            "^(?:logos|premios)/[1-9][0-9]*/"
                    + "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-"
                    + "[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}\\.(?:jpg|jpeg|png|webp)$"
    );

    private ArchivoSeguro() {
    }

    public static String validarImagen(MultipartFile archivo) {
        return validarMultipart(archivo, false);
    }

    public static String validarComprobante(MultipartFile archivo) {
        return validarMultipart(archivo, true);
    }

    public static String validarComprobante(String contentType, byte[] contenido) {
        return validarContenido(contentType, contenido, true);
    }

    public static boolean esImagenRasterSegura(String contentType) {
        String tipo = normalizarContentType(contentType);
        return JPEG.equals(tipo) || PNG.equals(tipo) || WEBP.equals(tipo);
    }

    public static void validarCarpetaMediaPublica(String carpeta) {
        if (!"logos".equals(carpeta) && !"premios".equals(carpeta)) {
            throw new SecurityException("Carpeta de media no permitida");
        }
    }

    public static boolean esReferenciaMediaPublica(String referencia) {
        return referencia != null && REFERENCIA_MEDIA_PUBLICA.matcher(referencia).matches();
    }

    public static String extension(String contentType) {
        return switch (normalizarContentType(contentType)) {
            case JPEG -> ".jpg";
            case PNG -> ".png";
            case WEBP -> ".webp";
            case PDF -> ".pdf";
            default -> "";
        };
    }

    public static String normalizarContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        String tipo = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return "image/jpg".equals(tipo) ? JPEG : tipo;
    }

    private static String validarMultipart(MultipartFile archivo, boolean permitirPdf) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo es obligatorio");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new IllegalArgumentException("El archivo no puede superar 5 MB");
        }
        try {
            return validarContenido(archivo.getContentType(), archivo.getBytes(), permitirPdf);
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo");
        }
    }

    private static String validarContenido(String contentType, byte[] contenido, boolean permitirPdf) {
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("El archivo es obligatorio");
        }
        if (contenido.length > TAMANO_MAXIMO) {
            throw new IllegalArgumentException("El archivo no puede superar 5 MB");
        }

        String declarado = normalizarContentType(contentType);
        String detectado = detectar(contenido);
        boolean tipoPermitido = esImagenRasterSegura(declarado) || (permitirPdf && PDF.equals(declarado));
        if (!tipoPermitido) {
            throw new IllegalArgumentException(permitirPdf
                    ? "El archivo debe ser JPG, PNG, WEBP o PDF"
                    : "El archivo debe ser JPG, PNG o WEBP");
        }
        if (!declarado.equals(detectado)) {
            throw new IllegalArgumentException("El contenido del archivo no coincide con su tipo");
        }
        return detectado;
    }

    private static String detectar(byte[] contenido) {
        if (empiezaCon(contenido, 0xFF, 0xD8, 0xFF)) {
            return JPEG;
        }
        if (empiezaCon(contenido, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return PNG;
        }
        if (contenido.length >= 12
                && empiezaCon(contenido, 0x52, 0x49, 0x46, 0x46)
                && empiezaConDesde(contenido, 8, 0x57, 0x45, 0x42, 0x50)) {
            return WEBP;
        }
        if (empiezaCon(contenido, 0x25, 0x50, 0x44, 0x46, 0x2D)) {
            return PDF;
        }
        return "";
    }

    private static boolean empiezaCon(byte[] contenido, int... firma) {
        return empiezaConDesde(contenido, 0, firma);
    }

    private static boolean empiezaConDesde(byte[] contenido, int inicio, int... firma) {
        if (contenido.length < inicio + firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((contenido[inicio + i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
