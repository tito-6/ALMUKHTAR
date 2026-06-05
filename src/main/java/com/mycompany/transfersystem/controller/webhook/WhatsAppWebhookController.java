package com.mycompany.transfersystem.controller.webhook;

import com.mycompany.transfersystem.service.notification.webhook.WhatsAppDeliveryWebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsAppWebhookController {

    private final WhatsAppDeliveryWebhookService webhookService;

    @org.springframework.beans.factory.annotation.Value("${almukhtar.whatsapp.webhook-verify-token:${whatsapp.webhook.verify-token:}}")
    private String verifyToken;

    public WhatsAppWebhookController(WhatsAppDeliveryWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Meta webhook verification handshake.
     */
    @GetMapping("/status")
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode") String mode,
            @RequestParam(name = "hub.verify_token") String token,
            @RequestParam(name = "hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && StringUtils.hasText(verifyToken) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/status")
    public ResponseEntity<Void> status(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {
        if (!webhookService.verifySignature(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            webhookService.handlePayload(rawBody);
        } catch (Exception e) {
            // Still return 200 to avoid Meta retry storms; log server-side.
            org.slf4j.LoggerFactory.getLogger(WhatsAppWebhookController.class)
                    .warn("WhatsApp webhook parse failed: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
