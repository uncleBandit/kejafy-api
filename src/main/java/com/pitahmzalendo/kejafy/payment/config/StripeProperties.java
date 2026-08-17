package com.pitahmzalendo.kejafy.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * app.stripe.secret-key=${STRIPE_SECRET_KEY}
 * app.stripe.publishable-key=${STRIPE_PUBLISHABLE_KEY}
 * app.stripe.webhook-secret=${STRIPE_WEBHOOK_SECRET}
 *
 * publishableKey is safe to hand to the client (CardPaymentInitiationResponse
 * includes it) — it's the secret key and webhook secret that must never
 * leave the server.
 */
@Component
@ConfigurationProperties(prefix = "app.stripe")
public class StripeProperties {

    private String secretKey;
    private String publishableKey;
    private String webhookSecret;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public void setPublishableKey(String publishableKey) {
        this.publishableKey = publishableKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }
}
