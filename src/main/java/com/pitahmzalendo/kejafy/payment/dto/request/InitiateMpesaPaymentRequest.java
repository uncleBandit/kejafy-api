package com.pitahmzalendo.kejafy.payment.dto.request;

import com.pitahmzalendo.kejafy.subscription.entity.SubscriptionPlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record InitiateMpesaPaymentRequest(
        @NotNull(message = "plan is required")
        SubscriptionPlan plan,

        @NotBlank(message = "phoneNumber is required")
        @Pattern(
                regexp = "^(0[71][0-9]{8}|\\+?254[71][0-9]{8})$",
                message = "phoneNumber must be a valid Safaricom number, e.g. 0712345678"
        )
        String phoneNumber
) {
}
