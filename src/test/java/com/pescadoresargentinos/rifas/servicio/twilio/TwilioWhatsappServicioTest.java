package com.pescadoresargentinos.rifas.servicio.twilio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pescadoresargentinos.rifas.configuracion.TwilioProperties;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void sigueLaRedireccionSeguraDeTwilioSinReenviarLasCredenciales() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<InputStream> redireccion = mock(HttpResponse.class);
        HttpResponse<InputStream> archivo = mock(HttpResponse.class);
        String cdnUrl = "https://mms.twiliocdn.com/media/documento?Signature=firma";
        byte[] contenido = "comprobante".getBytes(StandardCharsets.UTF_8);

        when(redireccion.statusCode()).thenReturn(307);
        when(redireccion.headers()).thenReturn(HttpHeaders.of(Map.of("Location", List.of(cdnUrl)), (a, b) -> true));
        when(redireccion.body()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(archivo.statusCode()).thenReturn(200);
        when(archivo.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Length", List.of(String.valueOf(contenido.length))), (a, b) -> true));
        when(archivo.body()).thenReturn(new ByteArrayInputStream(contenido));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(redireccion, archivo);

        TwilioWhatsappServicio servicio = new TwilioWhatsappServicio(
                properties(), new ObjectMapper(), RestClient.builder(), httpClient
        );
        String mediaUrl = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID
                + "/Messages/MM0123456789abcdef0123456789abcdef/Media/ME0123456789abcdef0123456789abcdef";

        assertThat(servicio.descargarMedia(mediaUrl)).isEqualTo(contenido);

        ArgumentCaptor<HttpRequest> requests = ArgumentCaptor.forClass(HttpRequest.class);
        org.mockito.Mockito.verify(httpClient, org.mockito.Mockito.times(2))
                .send(requests.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(requests.getAllValues().get(0).headers().firstValue("Authorization")).isPresent();
        assertThat(requests.getAllValues().get(1).uri().toString()).isEqualTo(cdnUrl);
        assertThat(requests.getAllValues().get(1).headers().firstValue("Authorization")).isEmpty();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void rechazaRedireccionesDeMediaFueraDelCdnDeTwilio() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<InputStream> redireccion = mock(HttpResponse.class);
        when(redireccion.statusCode()).thenReturn(302);
        when(redireccion.headers()).thenReturn(HttpHeaders.of(
                Map.of("Location", List.of("https://evil.example/documento")), (a, b) -> true
        ));
        when(redireccion.body()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(redireccion);
        TwilioWhatsappServicio servicio = new TwilioWhatsappServicio(
                properties(), new ObjectMapper(), RestClient.builder(), httpClient
        );
        String mediaUrl = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID
                + "/Messages/MM0123456789abcdef0123456789abcdef/Media/ME0123456789abcdef0123456789abcdef";

        assertThatThrownBy(() -> servicio.descargarMedia(mediaUrl))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Redireccion de media de Twilio invalida");
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
