package com.pescadoresargentinos.rifas.servicio.storage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class R2StorageIsolationValidatorTest {

    @Test
    void rechazaUnBucketCompartidoNoHabilitado() {
        R2StorageIsolationValidator validator = new R2StorageIsolationValidator(
                "rifas-storage",
                "rifas-storage",
                false,
                ""
        );

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("habilitarse explicitamente");
    }

    @Test
    void aceptaUnBucketCompartidoPrivado() {
        R2StorageIsolationValidator validator = new R2StorageIsolationValidator(
                "rifas-storage",
                "rifas-storage",
                true,
                ""
        );

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void rechazaExponerDirectamenteUnBucketCompartido() {
        R2StorageIsolationValidator validator = new R2StorageIsolationValidator(
                "rifas-storage",
                "rifas-storage",
                true,
                "https://media.example.test"
        );

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MEDIA_PUBLIC_BASE_URL");
    }

    @Test
    void aceptaBucketsSeparadosSinHabilitarElModoCompartido() {
        R2StorageIsolationValidator validator = new R2StorageIsolationValidator(
                "rifas-media",
                "rifas-comprobantes",
                false,
                "https://media.example.test"
        );

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }
}
