package com.pescadoresargentinos.rifas.servicio.twilio;

public record TwilioEnvioResultado(
        String estado,
        String messageSid,
        String error
) {
}
