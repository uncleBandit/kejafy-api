package com.pitahmzalendo.kejafy.payment.entity;

public enum PaymentStatus {
    // Initiated with the provider, waiting on the customer (STK prompt
    // approval) or on the provider (card processing) to resolve it.
    PENDING,
    SUCCEEDED,
    FAILED,
    // Left PENDING too long with no callback/webhook — see
    // PaymentReconciliationService. Distinct from FAILED because the
    // provider never actually told us it failed; we gave up waiting.
    EXPIRED
}
