package com.pescadoresargentinos.rifas.servicio.twilio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pescadoresargentinos.rifas.configuracion.TwilioProperties;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

class TwilioWhatsappServicioTest {

    private static final String ACCOUNT_SID = "test-account-sid";
    private static final String AUTH_TOKEN = "token-de-prueba";

    @Test
    void rechazaFirmasCuandoLaValidacionEstaDeshabilitada() {
        TwilioProperties properties = properties();
        properties.setValidateSignature(false);
        TwilioWhatsappServicio servicio = servicio(properties);

        assertThat(servicio.validarFirma("https://example.test/webhook", new LinkedMultiValueMap<>(), "firma"))
                .isFalse();
    }

    @Test
    void rechazaUrlsDeMediaFueraDeLaApiDeTwilio() {
        TwilioWhatsappServicio servicio = servicio(properties());

        assertThatThrownBy(() -> servicio.validarMediaUrl("http://169.254.169.254/latest/meta-data"))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> servicio.validarMediaUrl(
                "https://evil.example/2010-04-01/Accounts/" + ACCOUNT_SID
                        + "/Messages/SM0123456789abcdef0123456789abcdef/Media/ME0123456789abcdef0123456789abcdef"
        )).isInstanceOf(SecurityException.class);
    }

    @Test
    void aceptaFirmaYUrlValidasDeTwilio() throws Exception {
        TwilioWhatsappServicio servicio = servicio(properties());
        String webhookUrl = "https://rifas.example/api/twilio/whatsapp/webhook";
        LinkedMultiValueMap<String, String> parametros = new LinkedMultiValueMap<>();
        parametros.add("From", "whatsapp:+5491112345678");
        parametros.add("NumMedia", "1");
        String firma = firmar(webhookUrl + "Fromwhatsapp:+5491112345678NumMedia1");

        assertThat(servicio.validarFirma(webhookUrl, parametros, firma)).isTrue();
        assertThat(servicio.validarMediaUrl(
                "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID
                        + "/Messages/SM0123456789abcdef0123456789abcdef/Media/ME0123456789abcdef0123456789abcdef"
        ).getHost()).isEqualTo("api.twilio.com");
    }

    private TwilioWhatsappServicio servicio(TwilioProperties properties) {
        return new TwilioWhatsappServicio(properties, new ObjectMapper(), RestClient.builder());
    }

    private TwilioProperties properties() {
        TwilioProperties properties = new TwilioProperties();
        properties.setEnabled(true);
        properties.setValidateSignature(true);
        properties.setAccountSid(ACCOUNT_SID);
        properties.setAuthToken(AUTH_TOKEN);
        return properties;
    }

    private String firmar(String base) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(AUTH_TOKEN.getBytes(), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(base.getBytes()));
    }
}
