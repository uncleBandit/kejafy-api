package com.pitahmzalendo.kejafy.payment.stripe;

import com.pitahmzalendo.kejafy.payment.config.StripeProperties;
import com.pitahmzalendo.kejafy.payment.exception.PaymentProviderException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Thin wrapper over the Stripe Java SDK — just PaymentIntents (card
 * checkout) and webhook signature verification, which is all this
 * platform needs Stripe for. See PaymentService for orchestration.
 */
@Component
public class StripeClient {

    private static final Logger log = LoggerFactory.getLogger(StripeClient.class);

    private final StripeProperties properties;

    public StripeClient(StripeProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a PaymentIntent for one checkout attempt. The returned
     * intent's client secret goes straight to the mobile/web client,
     * which confirms it with Stripe's SDK (Payment Sheet / Elements) —
     * card details never pass through our backend.
     *
     * metadata carries our own PaymentTransaction id and the plan, so
     * the webhook handler can find its way back to the right row
     * without trusting anything the client sends at confirmation time.
     */
    public PaymentIntent createPaymentIntent(BigDecimal amount, String currency,
                                             Map<String, String> metadata) {
        // Stripe wants the amount in the currency's smallest unit
        // (cents). KES has no minor unit in everyday use, but Stripe
        // still treats it as two-decimal for API purposes.
        long amountInSmallestUnit = amount.movePointRight(2).longValueExact();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInSmallestUnit)
                .setCurrency(currency.toLowerCase())
                .putAllMetadata(metadata)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        try {
            return PaymentIntent.create(params);
        } catch (StripeException ex) {
            log.error("Failed to create Stripe PaymentIntent", ex);
            throw new PaymentProviderException("Could not start the card payment. Please try again shortly.", ex);
        }
    }

    /**
     * Used by PaymentReconciliationService to check on a PaymentIntent
     * directly when we haven't received a webhook for it — Stripe
     * webhooks are reliable in practice, but not guaranteed, and this
     * is the fallback for the rare miss.
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException ex) {
            log.error("Failed to retrieve Stripe PaymentIntent id={}", paymentIntentId, ex);
            throw new PaymentProviderException("Could not check card payment status.", ex);
        }
    }

    /**
     * Verifies the Stripe-Signature header against the raw request body
     * using the webhook signing secret. Throws if the payload was
     * tampered with or didn't actually come from Stripe — callers must
     * not process the event if this throws.
     */
    public Event parseWebhookEvent(String payload, String signatureHeader) {
        try {
            return Webhook.constructEvent(payload, signatureHeader, properties.getWebhookSecret());
        } catch (SignatureVerificationException ex) {
            log.warn("Rejected Stripe webhook with invalid signature");
            throw new PaymentProviderException("Invalid webhook signature.", ex);
        }
    }
}
