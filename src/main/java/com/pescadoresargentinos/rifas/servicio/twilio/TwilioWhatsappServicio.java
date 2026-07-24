package com.pescadoresargentinos.rifas.servicio.twilio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pescadoresargentinos.rifas.configuracion.TwilioProperties;
import com.pescadoresargentinos.rifas.dominio.Cliente;
import com.pescadoresargentinos.rifas.dominio.Compra;
import com.pescadoresargentinos.rifas.dominio.NumeroRifa;
import com.pescadoresargentinos.rifas.servicio.storage.ArchivoSeguro;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class TwilioWhatsappServicio {

    private static final Duration MEDIA_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration MEDIA_REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Pattern MENSAJE_Y_MEDIA_PATH = Pattern.compile(
            "[A-Z]{2}[0-9a-fA-F]{32}/Media/ME[0-9a-fA-F]{32}"
    );

    private final TwilioProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final HttpClient mediaHttpClient;

    public TwilioWhatsappServicio(TwilioProperties properties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.mediaHttpClient = HttpClient.newBuilder()
                .connectTimeout(MEDIA_CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
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
        if (!properties.credencialesConfiguradas()) {
            throw new SecurityException("Twilio no esta configurado");
        }
        URI uri = validarMediaUrl(mediaUrl);
        String credenciales = Base64.getEncoder().encodeToString(
                (properties.getAccountSid() + ":" + properties.getAuthToken()).getBytes(StandardCharsets.UTF_8)
        );
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(MEDIA_REQUEST_TIMEOUT)
                .header("Authorization", "Basic " + credenciales)
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = mediaHttpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response.body().close();
                throw new IllegalStateException("Twilio no devolvio el archivo solicitado");
            }
            long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            if (contentLength > ArchivoSeguro.TAMANO_MAXIMO) {
                response.body().close();
                throw new IllegalArgumentException("El comprobante no puede superar 5 MB");
            }
            try (InputStream contenido = response.body()) {
                byte[] bytes = contenido.readNBytes((int) ArchivoSeguro.TAMANO_MAXIMO + 1);
                if (bytes.length > ArchivoSeguro.TAMANO_MAXIMO) {
                    throw new IllegalArgumentException("El comprobante no puede superar 5 MB");
                }
                return bytes;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpio la descarga del comprobante");
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo descargar el comprobante desde Twilio");
        }
    }

    public boolean validarFirma(String url, MultiValueMap<String, String> parametros, String firmaRecibida) {
        if (!properties.isEnabled()
                || !properties.isValidateSignature()
                || !properties.credencialesConfiguradas()
                || esVacio(firmaRecibida)) {
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

    URI validarMediaUrl(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            throw new SecurityException("URL de media de Twilio invalida");
        }

        try {
            URI uri = URI.create(mediaUrl);
            String prefijo = "/2010-04-01/Accounts/" + properties.getAccountSid() + "/Messages/";
            String resto = uri.getPath() != null && uri.getPath().startsWith(prefijo)
                    ? uri.getPath().substring(prefijo.length())
                    : "";
            boolean segura = "https".equalsIgnoreCase(uri.getScheme())
                    && "api.twilio.com".equalsIgnoreCase(uri.getHost())
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && uri.getRawUserInfo() == null
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null
                    && MENSAJE_Y_MEDIA_PATH.matcher(resto).matches();
            if (!segura) {
                throw new SecurityException("URL de media de Twilio invalida");
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new SecurityException("URL de media de Twilio invalida");
        }
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
                : "\nSi tenes alguna duda contactate aca: https://wa.me/" + cliente.getWhatsappConsultas();
        String filasONumeros = compra.getEtiquetasNumeros().isEmpty()
                ? numerosCompra(compra)
                : String.join(", ", compra.getEtiquetasNumeros());
        return "Hola " + compra.getComprador().getNombre()
                + ", jugaste las Filas/numeros: " + filasONumeros
                + " en " + compra.getRifa().getTitulo() + "."
                + "\nMonto: " + dinero(compra.getTotal()) + "."
                + "\nAlias: " + compra.getRifa().getAliasTransferencia() + "."
                + "\nPodes enviar el comprobante por este mismo medio."
                + "\nENVIAR UNICAMENTE EL DOCUMENTO O IMAGEN DEL COMPROBANTE."
                + consultas;
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
