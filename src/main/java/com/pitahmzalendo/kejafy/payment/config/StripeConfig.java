package com.pitahmzalendo.kejafy.payment.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * The Stripe Java SDK reads its API key from a static field
 * (Stripe.apiKey) rather than a client instance you pass around — this
 * is the one place that gets set, at startup, from config rather than
 * hardcoded.
 */
@Component
public class StripeConfig {

    private final StripeProperties stripeProperties;

    public StripeConfig(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeProperties.getSecretKey();
    }
}
