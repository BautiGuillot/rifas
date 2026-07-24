package com.pescadoresargentinos.rifas.configuracion;

import java.net.URI;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ConfiguracionProduccionValidator implements InitializingBean {

    private final Environment environment;

    public ConfiguracionProduccionValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        String databaseUrl = requerida("spring.datasource.url");
        if (!databaseUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("Produccion requiere una base PostgreSQL");
        }
        requerida("spring.datasource.username");
        requerida("spring.datasource.password");

        String jwtSecret = requerida("app.jwt.secret");
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        if (requerida("app.super-admin.password").length() < 12) {
            throw new IllegalStateException("SUPER_ADMIN_PASSWORD debe tener al menos 12 caracteres");
        }

        exigirValor("spring.jpa.hibernate.ddl-auto", "validate");
        exigirValor("spring.h2.console.enabled", "false");
        exigirValor("springdoc.swagger-ui.enabled", "false");
        exigirValor("springdoc.api-docs.enabled", "false");
        exigirValor("server.error.include-message", "never");
        exigirValor("server.error.include-stacktrace", "never");
        exigirValor("app.comprobantes.storage", "r2");
        exigirValor("app.media.storage", "r2");

        String[] corsAllowedOrigins = requerida("app.cors.allowed-origins").split(",");
        for (String origin : corsAllowedOrigins) {
            validarOrigenCors(origin.trim());
        }

        String comprobantesBucket = requerida("app.comprobantes.r2.bucket");
        String mediaBucket = requerida("app.media.r2.bucket");
        if (comprobantesBucket.equalsIgnoreCase(mediaBucket)) {
            exigirValor("app.r2.shared-bucket", "true");
            String publicBaseUrl = environment.getProperty("app.media.public-base-url", "");
            if (!publicBaseUrl.isBlank()) {
                throw new IllegalStateException(
                        "MEDIA_PUBLIC_BASE_URL debe permanecer vacio con un bucket compartido"
                );
            }
        }
        requerida("app.comprobantes.r2.endpoint");
        requerida("app.comprobantes.r2.access-key");
        requerida("app.comprobantes.r2.secret-key");
        requerida("app.media.r2.endpoint");
        requerida("app.media.r2.access-key");
        requerida("app.media.r2.secret-key");

        validarHttpsOpcional("app.media.public-base-url");
        if (Boolean.parseBoolean(environment.getProperty("app.twilio.enabled", "false"))) {
            exigirValor("app.twilio.validate-signature", "true");
            requerida("app.twilio.account-sid");
            requerida("app.twilio.auth-token");
            validarHttpsRequerido("app.twilio.webhook-base-url");
        }
    }

    private String requerida(String propiedad) {
        String valor = environment.getProperty(propiedad);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta configurar " + propiedad + " para produccion");
        }
        return valor.trim();
    }

    private void exigirValor(String propiedad, String esperado) {
        String actual = requerida(propiedad);
        if (!esperado.equalsIgnoreCase(actual)) {
            throw new IllegalStateException(propiedad + " debe ser " + esperado + " en produccion");
        }
    }

    private void validarHttpsOpcional(String propiedad) {
        String valor = environment.getProperty(propiedad);
        if (valor != null && !valor.isBlank()) {
            validarHttps(propiedad, valor);
        }
    }

    private void validarHttpsRequerido(String propiedad) {
        validarHttps(propiedad, requerida(propiedad));
    }

    private void validarHttps(String propiedad, String valor) {
        try {
            URI uri = URI.create(valor);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalStateException(propiedad + " debe ser una URL HTTPS valida");
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(propiedad + " debe ser una URL HTTPS valida");
        }
    }

    private void validarOrigenCors(String valor) {
        String propiedad = "app.cors.allowed-origins";
        if (valor.isBlank() || valor.contains("*")) {
            throw new IllegalStateException(propiedad + " debe contener origenes HTTPS explicitos");
        }

        try {
            URI uri = URI.create(valor);
            boolean tieneRuta = uri.getRawPath() != null && !uri.getRawPath().isEmpty();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getRawUserInfo() != null
                    || tieneRuta
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null) {
                throw new IllegalStateException(
                        propiedad + " debe contener origenes HTTPS sin ruta"
                );
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    propiedad + " debe contener origenes HTTPS validos"
            );
        }
    }
}
