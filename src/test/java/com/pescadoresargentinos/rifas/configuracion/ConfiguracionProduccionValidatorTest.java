package com.pescadoresargentinos.rifas.configuracion;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ConfiguracionProduccionValidatorTest {

    @Test
    void aceptaUnaConfiguracionProductivaSegura() {
        ConfiguracionProduccionValidator validator = new ConfiguracionProduccionValidator(
                entornoProduccionSeguro()
        );

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void rechazaUnaBaseQueNoSeaPostgresql() {
        MockEnvironment environment = entornoProduccionSeguro()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:produccion");

        assertThatThrownBy(() -> new ConfiguracionProduccionValidator(environment).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PostgreSQL");
    }

    @Test
    void rechazaHerramientasDeDesarrolloExpuestas() {
        MockEnvironment environment = entornoProduccionSeguro()
                .withProperty("springdoc.swagger-ui.enabled", "true");

        assertThatThrownBy(() -> new ConfiguracionProduccionValidator(environment).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("springdoc.swagger-ui.enabled");
    }

    @Test
    void rechazaUnaUrlPublicaParaElBucketCompartido() {
        MockEnvironment environment = entornoProduccionSeguro()
                .withProperty("app.media.public-base-url", "https://media.example.test");

        assertThatThrownBy(() -> new ConfiguracionProduccionValidator(environment).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MEDIA_PUBLIC_BASE_URL");
    }

    @Test
    void rechazaCorsConWildcard() {
        MockEnvironment environment = entornoProduccionSeguro()
                .withProperty("app.cors.allowed-origins", "*");

        assertThatThrownBy(() -> new ConfiguracionProduccionValidator(environment).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cors.allowed-origins");
    }

    @Test
    void rechazaCorsSinHttps() {
        MockEnvironment environment = entornoProduccionSeguro()
                .withProperty("app.cors.allowed-origins", "http://frontend.example.test");

        assertThatThrownBy(() -> new ConfiguracionProduccionValidator(environment).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }

    private MockEnvironment entornoProduccionSeguro() {
        return new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/rifas")
                .withProperty("spring.datasource.username", "rifas")
                .withProperty("spring.datasource.password", "database-secret")
                .withProperty("app.jwt.secret", "jwt-secret-with-at-least-thirty-two-characters")
                .withProperty("app.super-admin.password", "super-admin-password")
                .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("spring.h2.console.enabled", "false")
                .withProperty("springdoc.swagger-ui.enabled", "false")
                .withProperty("springdoc.api-docs.enabled", "false")
                .withProperty("server.error.include-message", "never")
                .withProperty("server.error.include-stacktrace", "never")
                .withProperty("app.cors.allowed-origins", "https://frontend.example.test")
                .withProperty("app.comprobantes.storage", "r2")
                .withProperty("app.media.storage", "r2")
                .withProperty("app.comprobantes.r2.bucket", "rifas-storage")
                .withProperty("app.media.r2.bucket", "rifas-storage")
                .withProperty("app.r2.shared-bucket", "true")
                .withProperty("app.comprobantes.r2.endpoint", "https://r2.example.test")
                .withProperty("app.comprobantes.r2.access-key", "comprobantes-access")
                .withProperty("app.comprobantes.r2.secret-key", "comprobantes-secret")
                .withProperty("app.media.r2.endpoint", "https://r2.example.test")
                .withProperty("app.media.r2.access-key", "media-access")
                .withProperty("app.media.r2.secret-key", "media-secret")
                .withProperty("app.media.public-base-url", "")
                .withProperty("app.twilio.enabled", "false");
    }
}
