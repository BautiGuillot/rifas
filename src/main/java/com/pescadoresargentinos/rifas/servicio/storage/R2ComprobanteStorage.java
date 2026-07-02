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
@ConditionalOnProperty(name = "app.comprobantes.storage", havingValue = "r2")
public class R2ComprobanteStorage implements ComprobanteStorage {

    private final String bucket;
    private final S3Client s3Client;

    public R2ComprobanteStorage(
            @Value("${app.comprobantes.r2.endpoint}") String endpoint,
            @Value("${app.comprobantes.r2.bucket}") String bucket,
            @Value("${app.comprobantes.r2.access-key}") String accessKey,
            @Value("${app.comprobantes.r2.secret-key}") String secretKey,
            @Value("${app.comprobantes.r2.region:auto}") String region
    ) {
        String endpointRequerido = requerido(endpoint, "app.comprobantes.r2.endpoint / R2_ENDPOINT");
        this.bucket = requerido(bucket, "app.comprobantes.r2.bucket / R2_BUCKET");
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                requerido(accessKey, "app.comprobantes.r2.access-key / R2_ACCESS_KEY"),
                requerido(secretKey, "app.comprobantes.r2.secret-key / R2_SECRET_KEY")
        );
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpointRequerido))
                .region(Region.of(requerido(region, "app.comprobantes.r2.region / R2_REGION")))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Override
    public ComprobanteGuardado guardar(Long compraId, MultipartFile archivo) {
        try {
            String nombreOriginal = nombreOriginal(archivo);
            String key = "comprobantes/" + compraId + "/" + UUID.randomUUID() + extension(nombreOriginal);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(archivo.getContentType())
                    .metadata(java.util.Map.of("nombre-original", nombreOriginal))
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(archivo.getBytes()));
            return new ComprobanteGuardado(key, nombreOriginal, archivo.getContentType());
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
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(referencia)
                .build();
        byte[] bytes = s3Client.getObjectAsBytes(request).asByteArray();
        return Optional.of(new ComprobanteArchivo(
                new ByteArrayResource(bytes),
                nombreSeguro(nombreOriginal),
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

    private String nombreSeguro(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            return "comprobante";
        }
        return nombreOriginal.replace("\"", "");
    }

    private String requerido(String valor, String propiedad) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta configurar " + propiedad + " para usar Cloudflare R2");
        }
        return valor;
    }
}
