package com.pescadoresargentinos.rifas.servicio.twilio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pescadoresargentinos.rifas.configuracion.TwilioProperties;
import com.pescadoresargentinos.rifas.dominio.Cliente;
import com.pescadoresargentinos.rifas.dominio.Compra;
import com.pescadoresargentinos.rifas.dominio.NumeroRifa;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class TwilioWhatsappServicio {

    private final TwilioProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public TwilioWhatsappServicio(TwilioProperties properties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    public TwilioEnvioResultado enviarMensajeCompra(Compra compra) {
        Cliente cliente = compra.getRifa().getCliente();
        if (!properties.isEnabled() || !properties.credencialesConfiguradas()) {
            return new TwilioEnvioResultado("NO_CONFIGURADO", null, null);
        }
        if (cliente == null || !Boolean.TRUE.equals(cliente.getTwilioWhatsappHabilitado())) {
            return new TwilioEnvioResultado("NO_CONFIGURADO", null, null);
        }
        if (esVacio(cliente.getTwilioWhatsappFrom()) && esVacio(cliente.getTwilioMessagingServiceSid())) {
            return new TwilioEnvioResultado("ERROR", null, "Falta configurar el remitente o Messaging Service de Twilio");
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", whatsappAddress(compra.getComprador().getTelefono()));
            if (!esVacio(cliente.getTwilioMessagingServiceSid())) {
                form.add("MessagingServiceSid", cliente.getTwilioMessagingServiceSid());
            } else {
                form.add("From", whatsappAddress(cliente.getTwilioWhatsappFrom()));
            }

            if (!esVacio(cliente.getTwilioContentSid())) {
                form.add("ContentSid", cliente.getTwilioContentSid());
                form.add("ContentVariables", variablesTemplate(compra, cliente));
            } else {
                form.add("Body", mensajeLibre(compra, cliente));
            }

            String response = restClient.post()
                    .uri("https://api.twilio.com/2010-04-01/Accounts/{accountSid}/Messages.json", properties.getAccountSid())
                    .headers(headers -> headers.setBasicAuth(properties.getAccountSid(), properties.getAuthToken(), StandardCharsets.UTF_8))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            return new TwilioEnvioResultado("ENVIADO", extraerSid(response), null);
        } catch (RuntimeException ex) {
            return new TwilioEnvioResultado("ERROR", null, limpiarError(ex));
        }
    }

    public byte[] descargarMedia(String mediaUrl) {
        return restClient.get()
                .uri(mediaUrl)
                .headers(headers -> headers.setBasicAuth(properties.getAccountSid(), properties.getAuthToken(), StandardCharsets.UTF_8))
                .retrieve()
                .body(byte[].class);
    }

    public boolean validarFirma(String url, MultiValueMap<String, String> parametros, String firmaRecibida) {
        if (!properties.isValidateSignature()) {
            return true;
        }
        if (esVacio(firmaRecibida) || esVacio(properties.getAuthToken())) {
            return false;
        }
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new javax.crypto.spec.SecretKeySpec(properties.getAuthToken().getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            StringBuilder base = new StringBuilder(url);
            parametros.keySet().stream().sorted().forEach(clave ->
                    parametros.get(clave).forEach(valor -> base.append(clave).append(valor))
            );
            String firmaCalculada = Base64.getEncoder().encodeToString(mac.doFinal(base.toString().getBytes(StandardCharsets.UTF_8)));
            return java.security.MessageDigest.isEqual(
                    firmaCalculada.getBytes(StandardCharsets.UTF_8),
                    firmaRecibida.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ex) {
            return false;
        }
    }

    public String webhookUrl(String requestUrl) {
        if (esVacio(properties.getWebhookBaseUrl())) {
            return requestUrl;
        }
        return properties.getWebhookBaseUrl().replaceAll("/+$", "") + "/api/twilio/whatsapp/webhook";
    }

    private String variablesTemplate(Compra compra, Cliente cliente) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("1", compra.getComprador().getNombre());
        variables.put("2", compra.getId().toString());
        variables.put("3", compra.getRifa().getTitulo());
        variables.put("4", compra.getEtiquetasNumeros().isEmpty() ? numerosCompra(compra) : String.join(", ", compra.getEtiquetasNumeros()));
        variables.put("5", dinero(compra.getTotal()));
        variables.put("6", compra.getRifa().getAliasTransferencia());
        variables.put("7", esVacio(cliente.getWhatsappConsultas()) ? "" : "https://wa.me/" + cliente.getWhatsappConsultas());
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo armar el mensaje de WhatsApp");
        }
    }

    private String mensajeLibre(Compra compra, Cliente cliente) {
        String consultas = esVacio(cliente.getWhatsappConsultas())
                ? ""
                : " Si tenes alguna duda contactate aca: https://wa.me/" + cliente.getWhatsappConsultas();
        return "Hola " + compra.getComprador().getNombre()
                + ", jugaste la reserva #" + compra.getId()
                + " en " + compra.getRifa().getTitulo()
                + ". Filas/numeros: " + (compra.getEtiquetasNumeros().isEmpty() ? numerosCompra(compra) : String.join(", ", compra.getEtiquetasNumeros()))
                + ". Monto: " + dinero(compra.getTotal())
                + ". Alias: " + compra.getRifa().getAliasTransferencia()
                + ". Envia el comprobante por este mismo medio. Unicamente leemos comprobantes." + consultas;
    }

    private String numerosCompra(Compra compra) {
        return compra.getNumeros().stream()
                .sorted(Comparator.comparing(NumeroRifa::getValor))
                .map(NumeroRifa::getEtiqueta)
                .toList()
                .stream()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private String dinero(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).format(valor);
    }

    private String extraerSid(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }
        try {
            JsonNode json = objectMapper.readTree(response);
            return json.path("sid").asText(null);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String whatsappAddress(String numero) {
        return "whatsapp:+" + numero.replace("whatsapp:", "").replace("+", "").replaceAll("\\D", "");
    }

    private String limpiarError(Exception ex) {
        String mensaje = ex.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            return "No se pudo enviar el WhatsApp automatico";
        }
        return mensaje.length() > 1000 ? mensaje.substring(0, 1000) : mensaje;
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
