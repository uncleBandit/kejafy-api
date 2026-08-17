package com.pitahmzalendo.kejafy.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Daraja's response body from POST /mpesa/stkpush/v1/processrequest.
 * ResponseCode "0" means the STK push was accepted and sent to the
 * phone — it says nothing about whether the customer actually approves
 * it. That result only arrives later via the callback (see
 * MpesaCallbackPayload) or a manual query (see MpesaClient#queryStkStatus).
 */
public record StkPushResponse(
        @JsonProperty("MerchantRequestID") String merchantRequestId,
        @JsonProperty("CheckoutRequestID") String checkoutRequestId,
        @JsonProperty("ResponseCode") String responseCode,
        @JsonProperty("ResponseDescription") String responseDescription,
        @JsonProperty("CustomerMessage") String customerMessage
) {
    public boolean accepted() {
        return "0".equals(responseCode);
    }
}
