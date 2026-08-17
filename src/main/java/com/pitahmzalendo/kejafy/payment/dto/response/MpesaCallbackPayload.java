package com.pitahmzalendo.kejafy.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

/**
 * Body Safaricom POSTs to app.mpesa.callback-url once the customer
 * approves or dismisses the STK prompt (or it times out on their end).
 * ResultCode 0 = paid; anything else = not paid, with resultDesc
 * explaining why (cancelled, insufficient funds, timeout, ...).
 * CallbackMetadata is only present on success.
 *
 * Field names are PascalCase in the wire format because that's what
 * Daraja actually sends — @JsonProperty maps them to normal Java
 * naming rather than the whole DTO tree fighting Jackson's defaults.
 */
public record MpesaCallbackPayload(
        @JsonProperty("Body") Body body
) {
    public record Body(
            @JsonProperty("stkCallback") StkCallback stkCallback
    ) {
    }

    public record StkCallback(
            @JsonProperty("MerchantRequestID") String merchantRequestId,
            @JsonProperty("CheckoutRequestID") String checkoutRequestId,
            @JsonProperty("ResultCode") int resultCode,
            @JsonProperty("ResultDesc") String resultDesc,
            @JsonProperty("CallbackMetadata") CallbackMetadata callbackMetadata
    ) {
        public boolean successful() {
            return resultCode == 0;
        }

        public Optional<String> mpesaReceiptNumber() {
            return metadataValue("MpesaReceiptNumber").map(String::valueOf);
        }

        public Optional<String> payerPhoneNumber() {
            return metadataValue("PhoneNumber").map(String::valueOf);
        }

        private Optional<String> metadataValue(String name) {
            if (callbackMetadata == null || callbackMetadata.item() == null) {
                return Optional.empty();
            }
            return callbackMetadata.item().stream()
                    .filter(item -> name.equals(item.name()))
                    .map(Item::value)
                    .filter(java.util.Objects::nonNull)
                    .map(Object::toString)
                    .findFirst();
        }
    }

    public record CallbackMetadata(
            @JsonProperty("Item") List<Item> item
    ) {
    }

    public record Item(
            @JsonProperty("Name") String name,
            @JsonProperty("Value") Object value
    ) {
    }
}
