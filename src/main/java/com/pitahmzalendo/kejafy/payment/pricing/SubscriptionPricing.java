package com.pitahmzalendo.kejafy.payment.pricing;

import com.pitahmzalendo.kejafy.subscription.entity.SubscriptionPlan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Single source of truth for "what does this plan cost" — both payment
 * flows (M-Pesa STK push amount, Stripe PaymentIntent amount) read from
 * here rather than hardcoding a figure each, so a price change is one
 * properties edit instead of a code change in two providers.
 *
 * Deliberately server-side only: never trust a price the client sends.
 * InitiateMpesaPaymentRequest/InitiateCardPaymentRequest only carry
 * which plan was picked — the amount is looked up here.
 *
 * app.subscription.pricing.monthly=999
 * app.subscription.pricing.yearly=9999
 * app.subscription.pricing.currency=KES
 */
@Component
@ConfigurationProperties(prefix = "app.subscription.pricing")
public class SubscriptionPricing {

    private BigDecimal monthly;
    private BigDecimal yearly;
    private String currency = "KES";

    public BigDecimal amountFor(SubscriptionPlan plan) {
        BigDecimal amount = switch (plan) {
            case MONTHLY -> monthly;
            case YEARLY -> yearly;
        };
        if (amount == null) {
            throw new IllegalStateException(
                    "No price configured for plan " + plan + " — set app.subscription.pricing.* .");
        }
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Map<SubscriptionPlan, BigDecimal> allPrices() {
        return Map.of(
                SubscriptionPlan.MONTHLY, monthly,
                SubscriptionPlan.YEARLY, yearly
        );
    }

    public BigDecimal getMonthly() {
        return monthly;
    }

    public void setMonthly(BigDecimal monthly) {
        this.monthly = monthly;
    }

    public BigDecimal getYearly() {
        return yearly;
    }

    public void setYearly(BigDecimal yearly) {
        this.yearly = yearly;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
