package com.pitahmzalendo.kejafy.payment.dto.request;

import com.pitahmzalendo.kejafy.subscription.entity.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;

public record InitiateCardPaymentRequest(
        @NotNull(message = "plan is required")
        SubscriptionPlan plan
) {
}
