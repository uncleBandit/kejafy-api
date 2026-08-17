package com.pitahmzalendo.kejafy.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from POST /mpesa/stkpushquery/v1/query — used by
 * PaymentReconciliationService to double-check a PENDING transaction
 * that never received a callback (dropped webhook, customer's network
 * flaked mid-prompt, etc). ResultCode is a numeric string here, unlike
 * the plain int Daraja sends in the async callback — that inconsistency
 * is Safaricom's, not a typo.
 */
public record StkStatusQueryResponse(
        @JsonProperty("ResponseCode") String responseCode,
        @JsonProperty("MerchantRequestID") String merchantRequestId,
        @JsonProperty("CheckoutRequestID") String checkoutRequestId,
        @JsonProperty("ResultCode") String resultCode,
        @JsonProperty("ResultDesc") String resultDesc
) {
    public boolean stillPending() {
        // 1032 = cancelled by user, 1037 = timeout, 1025/9999 = various
        // gateway errors — Daraja uses 500 for "still being processed"
        // in some sandbox versions and a blank/absent code in others,
        // so treat anything that isn't a clean 0 (paid) as needing
        // another look rather than assuming failure.
        return resultCode == null || resultCode.isBlank();
    }

    public boolean paid() {
        return "0".equals(resultCode);
    }
}
