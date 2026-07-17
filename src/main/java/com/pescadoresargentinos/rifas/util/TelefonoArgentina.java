package com.pescadoresargentinos.rifas.util;

public final class TelefonoArgentina {

    private static final String PREFIJO_CELULAR = "549";

    private TelefonoArgentina() {
    }

    public static String normalizarObligatorio(String telefono) {
        String normalizado = normalizar(telefono);
        if (normalizado == null) {
            throw new IllegalArgumentException("El celular debe tener entre 8 y 10 digitos, sin el 0 ni el 15");
        }
        return normalizado;
    }

    public static String normalizarOpcional(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return null;
        }
        return normalizarObligatorio(telefono);
    }

    private static String normalizar(String telefono) {
        if (telefono == null) {
            return null;
        }
        String digitos = telefono.replace("whatsapp:", "").replaceAll("\\D", "");
        if (digitos.startsWith(PREFIJO_CELULAR)) {
            return esCelularValido(digitos.substring(PREFIJO_CELULAR.length())) ? digitos : null;
        }
        if (digitos.startsWith("54")) {
            digitos = digitos.substring(2);
            if (digitos.startsWith("9")) {
                digitos = digitos.substring(1);
            }
        }
        if (digitos.startsWith("0")) {
            digitos = digitos.substring(1);
        }
        return esCelularValido(digitos) ? PREFIJO_CELULAR + digitos : null;
    }

    private static boolean esCelularValido(String digitos) {
        return digitos.matches("[1-9][0-9]{7,9}");
    }
}
