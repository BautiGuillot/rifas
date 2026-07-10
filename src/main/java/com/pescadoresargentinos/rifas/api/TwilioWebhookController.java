package com.pescadoresargentinos.rifas.api;

import com.pescadoresargentinos.rifas.servicio.twilio.TwilioWebhookServicio;
import com.pescadoresargentinos.rifas.servicio.twilio.TwilioWhatsappServicio;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/twilio/whatsapp")
public class TwilioWebhookController {

    private final TwilioWebhookServicio twilioWebhookServicio;
    private final TwilioWhatsappServicio twilioWhatsappServicio;

    public TwilioWebhookController(TwilioWebhookServicio twilioWebhookServicio, TwilioWhatsappServicio twilioWhatsappServicio) {
        this.twilioWebhookServicio = twilioWebhookServicio;
        this.twilioWhatsappServicio = twilioWhatsappServicio;
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> recibir(
            @RequestParam MultiValueMap<String, String> parametros,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String firma,
            HttpServletRequest request
    ) {
        String url = twilioWhatsappServicio.webhookUrl(request.getRequestURL().toString());
        if (!twilioWhatsappServicio.validarFirma(url, parametros, firma)) {
            return ResponseEntity.status(403).body("<Response/>");
        }
        twilioWebhookServicio.procesar(parametros);
        return ResponseEntity.ok("<Response/>");
    }
}
