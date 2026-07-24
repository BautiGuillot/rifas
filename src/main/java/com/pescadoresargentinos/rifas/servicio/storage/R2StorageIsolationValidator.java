package com.pescadoresargentinos.rifas.servicio.storage;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = {"app.media.storage", "app.comprobantes.storage"},
        havingValue = "r2"
)
public class R2StorageIsolationValidator implements InitializingBean {

    private final String mediaBucket;
    private final String comprobantesBucket;
    private final boolean sharedBucket;
    private final String mediaPublicBaseUrl;

    public R2StorageIsolationValidator(
            @Value("${app.media.r2.bucket}") String mediaBucket,
            @Value("${app.comprobantes.r2.bucket}") String comprobantesBucket,
            @Value("${app.r2.shared-bucket:false}") boolean sharedBucket,
            @Value("${app.media.public-base-url:}") String mediaPublicBaseUrl
    ) {
        this.mediaBucket = mediaBucket;
        this.comprobantesBucket = comprobantesBucket;
        this.sharedBucket = sharedBucket;
        this.mediaPublicBaseUrl = mediaPublicBaseUrl;
    }

    @Override
    public void afterPropertiesSet() {
        if (mediaBucket == null || mediaBucket.isBlank()) {
            throw new IllegalStateException("Falta configurar el bucket R2 de media publica");
        }
        if (comprobantesBucket == null || comprobantesBucket.isBlank()) {
            throw new IllegalStateException("Falta configurar el bucket R2 privado de comprobantes");
        }
        boolean mismoBucket = mediaBucket.trim().equalsIgnoreCase(comprobantesBucket.trim());
        if (mismoBucket && !sharedBucket) {
            throw new IllegalStateException(
                    "El bucket R2 compartido debe habilitarse explicitamente"
            );
        }
        if (mismoBucket && mediaPublicBaseUrl != null && !mediaPublicBaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "Un bucket con comprobantes no puede exponerse mediante MEDIA_PUBLIC_BASE_URL"
            );
        }
    }
}
