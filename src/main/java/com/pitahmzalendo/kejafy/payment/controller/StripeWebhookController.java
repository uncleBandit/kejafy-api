package com.pitahmzalendo.kejafy.payment.controller;

import com.pitahmzalendo.kejafy.payment.exception.PaymentProviderException;
import com.pitahmzalendo.kejafy.payment.service.PaymentService;
import com.pitahmzalendo.kejafy.payment.stripe.StripeClient;
import com.stripe.model.Event;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint Stripe posts payment events to — configured as the webhook
 * URL in the Stripe Dashboard, set as app.stripe.webhook-secret here.
 * permitAll in SecurityConfig, same as the M-Pesa callback, but Stripe
 * webhooks ARE cryptographically signed (Stripe-Signature header), so a
 * valid signature is sufficient trust on its own — no secondary
 * confirmation call needed the way M-Pesa's callback requires one.
 *
 * The raw request body is taken as a String specifically because
 * signature verification (Webhook.constructEvent) needs the exact bytes
 * Stripe sent — if this were a @RequestBody Event, Jackson would have
 * already re-serialized it and the signature check would fail.
 */
@RestController
@Hidden
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeClient stripeClient;
    private final PaymentService paymentService;

    public StripeWebhookController(StripeClient stripeClient, PaymentService paymentService) {
        this.stripeClient = stripeClient;
        this.paymentService = paymentService;
    }

    @PostMapping("/api/v1/payments/stripe/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader
    ) {
        Event event;
        try {
            event = stripeClient.parseWebhookEvent(payload, signatureHeader);
        } catch (PaymentProviderException ex) {
            // Bad/missing signature — reject outright, unlike the M-Pesa
            // callback there is no legitimate reason for this to fail.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            paymentService.handleStripeWebhook(event);
        } catch (Exception ex) {
            log.error("Error processing Stripe webhook event id={}", event.getId(), ex);
            // 500 here is intentional (unlike the M-Pesa callback):
            // Stripe retries failed webhook deliveries with backoff, and
            // markSucceeded/markFailed are idempotent, so it's safe to
            // let Stripe try again rather than silently swallowing it.
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }
}
