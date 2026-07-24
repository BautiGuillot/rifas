package com.pescadoresargentinos.rifas.servicio.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ArchivoSeguroTest {

    @Test
    void rechazaSvgAunqueSeDeclareComoImagen() {
        MockMultipartFile svg = new MockMultipartFile(
                "archivo",
                "ataque.svg",
                "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes()
        );

        assertThatThrownBy(() -> ArchivoSeguro.validarComprobante(svg))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaContenidoQueNoCoincideConElMimeDeclarado() {
        MockMultipartFile falsoPng = new MockMultipartFile(
                "archivo",
                "ataque.png",
                "image/png",
                "<html><script>alert(1)</script></html>".getBytes()
        );

        assertThatThrownBy(() -> ArchivoSeguro.validarComprobante(falsoPng))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no coincide");
    }

    @Test
    void aceptaPngCuandoLaFirmaEsCorrecta() {
        byte[] pngMinimo = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
        };
        MockMultipartFile png = new MockMultipartFile("archivo", "comprobante.png", "image/png", pngMinimo);

        assertThat(ArchivoSeguro.validarComprobante(png)).isEqualTo("image/png");
    }

    @Test
    void soloAceptaReferenciasDeMediaEnNamespacesPublicos() {
        assertThat(ArchivoSeguro.esReferenciaMediaPublica(
                "logos/12/123e4567-e89b-42d3-a456-426614174000.webp"
        )).isTrue();
        assertThat(ArchivoSeguro.esReferenciaMediaPublica(
                "premios/7/123e4567-e89b-42d3-a456-426614174000.jpg"
        )).isTrue();

        assertThat(ArchivoSeguro.esReferenciaMediaPublica(
                "comprobantes/12/123e4567-e89b-42d3-a456-426614174000.pdf"
        )).isFalse();
        assertThat(ArchivoSeguro.esReferenciaMediaPublica(
                "logos/12/../../comprobantes/archivo.pdf"
        )).isFalse();
        assertThat(ArchivoSeguro.esReferenciaMediaPublica(
                "logos/12/123e4567-e89b-42d3-a456-426614174000.svg"
        )).isFalse();
    }

    @Test
    void rechazaCarpetasDeCargaQueNoSeanPublicas() {
        assertThatThrownBy(() -> ArchivoSeguro.validarCarpetaMediaPublica("comprobantes"))
                .isInstanceOf(SecurityException.class);
    }
}
