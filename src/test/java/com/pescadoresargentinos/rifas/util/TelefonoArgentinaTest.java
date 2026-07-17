package com.pescadoresargentinos.rifas.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TelefonoArgentinaTest {

    @Test
    void normalizaElCelularConOtrasPresentacionesComunes() {
        assertEquals("5492364379198", TelefonoArgentina.normalizarObligatorio("2364379198"));
        assertEquals("5492364379198", TelefonoArgentina.normalizarObligatorio("+5492364379198"));
        assertEquals("5492364379198", TelefonoArgentina.normalizarObligatorio("54 9 2364-379198"));
    }

    @Test
    void rechazaUnNumeroQueNoTieneLongitudDeCelularArgentino() {
        assertThrows(IllegalArgumentException.class, () -> TelefonoArgentina.normalizarObligatorio("12345"));
    }
}
