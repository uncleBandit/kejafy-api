package com.pitahmzalendo.kejafy.payment.dto.response;

import com.pitahmzalendo.kejafy.payment.entity.PaymentTransaction;

/**
 * Returned right after triggering the STK push. The client should poll
 * GET /api/v1/payments/{transactionId}/status (see PaymentStatusResponse)
 * until it moves off PENDING — there's no redirect/callback to the
 * client itself, only the server-to-server callback from Safaricom.
 */
public record MpesaPaymentInitiationResponse(
        Long transactionId,
        String checkoutRequestId,
        String message
) {
    public static MpesaPaymentInitiationResponse from(PaymentTransaction transaction, String customerMessage) {
        return new MpesaPaymentInitiationResponse(
                transaction.getId(),
                transaction.getProviderReference(),
                customerMessage
        );
    }
}
