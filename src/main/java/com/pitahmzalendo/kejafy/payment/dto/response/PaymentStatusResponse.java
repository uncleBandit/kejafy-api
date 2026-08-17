package com.pitahmzalendo.kejafy.payment.dto.response;

import com.pitahmzalendo.kejafy.payment.entity.PaymentProvider;
import com.pitahmzalendo.kejafy.payment.entity.PaymentStatus;
import com.pitahmzalendo.kejafy.payment.entity.PaymentTransaction;
import com.pitahmzalendo.kejafy.subscription.entity.SubscriptionPlan;

import java.math.BigDecimal;

public record PaymentStatusResponse(
        Long transactionId,
        PaymentProvider provider,
        PaymentStatus status,
        SubscriptionPlan plan,
        BigDecimal amount,
        String currency,
        String providerReceipt,
        String failureReason
) {
    public static PaymentStatusResponse from(PaymentTransaction transaction) {
        return new PaymentStatusResponse(
                transaction.getId(),
                transaction.getProvider(),
                transaction.getStatus(),
                transaction.getPlan(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getProviderReceipt(),
                transaction.getFailureReason()
        );
    }
}
