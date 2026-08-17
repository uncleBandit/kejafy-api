package com.pitahmzalendo.kejafy.payment.dto.response;

import com.pitahmzalendo.kejafy.payment.entity.PaymentTransaction;

/**
 * clientSecret goes straight into Stripe's client-side SDK (Payment
 * Sheet on mobile, Elements on web) to collect and confirm the card —
 * card details never touch our backend. publishableKey is safe to ship
 * to the client; it's paired with the secret key server-side only.
 */
public record CardPaymentInitiationResponse(
        Long transactionId,
        String clientSecret,
        String publishableKey
) {
    public static CardPaymentInitiationResponse from(PaymentTransaction transaction, String clientSecret,
                                                     String publishableKey) {
        return new CardPaymentInitiationResponse(transaction.getId(), clientSecret, publishableKey);
    }
}
