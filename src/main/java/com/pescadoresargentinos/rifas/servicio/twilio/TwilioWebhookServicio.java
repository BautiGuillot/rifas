package com.pescadoresargentinos.rifas.servicio.twilio;

import com.pescadoresargentinos.rifas.dominio.Compra;
import com.pescadoresargentinos.rifas.servicio.CompraServicio;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
public class TwilioWebhookServicio {

    private static final Pattern COMPRA_ID_PATTERN = Pattern.compile("(?:#|reserva\\s*)(\\d+)", Pattern.CASE_INSENSITIVE);

    private final CompraServicio compraServicio;
    private final TwilioWhatsappServicio twilioWhatsappServicio;

    public TwilioWebhookServicio(CompraServicio compraServicio, TwilioWhatsappServicio twilioWhatsappServicio) {
        this.compraServicio = compraServicio;
        this.twilioWhatsappServicio = twilioWhatsappServicio;
    }

    public boolean procesar(MultiValueMap<String, String> parametros) {
        int cantidadMedia = entero(parametros.getFirst("NumMedia"));
        if (cantidadMedia <= 0) {
            return false;
        }
        Optional<Long> compraId = extraerCompraId(parametros.getFirst("Body"));
        Optional<Compra> compra = compraId
                .flatMap(compraServicio::buscarCompraPendientePorId)
                .or(() -> compraServicio.buscarCompraPendientePorWhatsapp(parametros.getFirst("To"), parametros.getFirst("From")));
        if (compra.isEmpty()) {
            return false;
        }

        String mediaUrl = parametros.getFirst("MediaUrl0");
        String contentType = parametros.getFirst("MediaContentType0");
        byte[] contenido = twilioWhatsappServicio.descargarMedia(mediaUrl);
        String extension = extension(contentType);
        return compraServicio.registrarComprobanteDesdeWhatsapp(compra.get().getId(), "comprobante-whatsapp" + extension, contentType, contenido);
    }

    private Optional<Long> extraerCompraId(String body) {
        if (body == null) {
            return Optional.empty();
        }
        Matcher matcher = COMPRA_ID_PATTERN.matcher(body);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Long.valueOf(matcher.group(1)));
    }

    private int entero(String valor) {
        try {
            return valor == null ? 0 : Integer.parseInt(valor);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String extension(String contentType) {
        if ("application/pdf".equals(contentType)) {
            return ".pdf";
        }
        if (contentType != null && contentType.startsWith("image/")) {
            return "." + contentType.substring("image/".length()).replace("jpeg", "jpg");
        }
        return "";
    }
}
