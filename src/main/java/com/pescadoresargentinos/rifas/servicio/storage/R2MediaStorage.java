package com.pescadoresargentinos.rifas.servicio.storage;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@ConditionalOnProperty(name = "app.media.storage", havingValue = "r2")
public class R2MediaStorage implements MediaStorage {

    private final String bucket;
    private final String publicBaseUrl;
    private final S3Client s3Client;

    public R2MediaStorage(
            @Value("${app.media.r2.endpoint}") String endpoint,
            @Value("${app.media.r2.bucket}") String bucket,
            @Value("${app.media.r2.access-key}") String accessKey,
            @Value("${app.media.r2.secret-key}") String secretKey,
            @Value("${app.media.r2.region:auto}") String region,
            @Value("${app.media.public-base-url:}") String publicBaseUrl
    ) {
        String endpointRequerido = requerido(endpoint, "app.media.r2.endpoint / R2_ENDPOINT");
        this.bucket = requerido(bucket, "app.media.r2.bucket / R2_BUCKET");
        this.publicBaseUrl = publicBaseUrl;
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                requerido(accessKey, "app.media.r2.access-key / R2_ACCESS_KEY"),
                requerido(secretKey, "app.media.r2.secret-key / R2_SECRET_KEY")
        );
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpointRequerido))
                .region(Region.of(requerido(region, "app.media.r2.region / R2_REGION")))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Override
    public MediaGuardado guardarImagen(String carpeta, Long ownerId, MultipartFile archivo) {
        try {
            validarImagen(archivo);
            String nombreOriginal = nombreOriginal(archivo);
            String key = carpeta + "/" + ownerId + "/" + UUID.randomUUID() + extension(nombreOriginal);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(archivo.getContentType())
                    .metadata(java.util.Map.of("nombre-original", nombreOriginal))
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(archivo.getBytes()));
            return new MediaGuardado(key, urlPublica(key), nombreOriginal, archivo.getContentType());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la imagen");
        }
    }

    @Override
    public Optional<ComprobanteArchivo> abrir(String referencia) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(referencia)
                .build();
        byte[] bytes = s3Client.getObjectAsBytes(request).asByteArray();
        return Optional.of(new ComprobanteArchivo(new ByteArrayResource(bytes), nombreSeguro(referencia), contentType(referencia)));
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

    private String nombreSeguro(String referencia) {
        int slash = referencia.lastIndexOf('/');
        return slash >= 0 ? referencia.substring(slash + 1) : referencia;
    }

    private String contentType(String referencia) {
        String lower = referencia.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private String requerido(String valor, String propiedad) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta configurar " + propiedad + " para usar Cloudflare R2");
        }
        return valor;
    }
}
