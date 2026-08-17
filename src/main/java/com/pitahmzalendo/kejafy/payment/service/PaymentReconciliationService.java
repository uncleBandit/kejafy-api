package com.pitahmzalendo.kejafy.payment.service;

import com.pitahmzalendo.kejafy.payment.entity.PaymentTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Safety net for the two ways a checkout can go silent: an M-Pesa
 * callback that Safaricom never delivers (dropped, or our callback URL
 * was briefly down), or — much rarer — a missed Stripe webhook. Without
 * this, a customer who genuinely paid could be stuck looking PENDING
 * forever with no subscription to show for it.
 *
 * Runs every 5 minutes; PaymentService.PENDING_TIMEOUT_MINUTES (10)
 * gives real in-flight checkouts room to resolve normally via their
 * callback/webhook before this ever looks at them.
 */
@Service
public class PaymentReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);

    private final PaymentService paymentService;

    public PaymentReconciliationService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Scheduled(fixedDelay = 5, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public void reconcileStalePendingTransactions() {
        List<PaymentTransaction> stale = paymentService.findStalePending();
        if (stale.isEmpty()) {
            return;
        }
        log.info("Reconciling {} stale-pending payment transaction(s)", stale.size());
        for (PaymentTransaction transaction : stale) {
            try {
                paymentService.reconcile(transaction.getId());
            } catch (Exception ex) {
                // One bad provider call shouldn't stop the rest of the
                // sweep — it'll be picked up again next run.
                log.error("Failed to reconcile payment transaction id={}", transaction.getId(), ex);
            }
        }
    }
}
