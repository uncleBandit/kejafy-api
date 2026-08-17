package com.pitahmzalendo.kejafy.payment.service;

import com.pitahmzalendo.kejafy.payment.dto.request.InitiateCardPaymentRequest;
import com.pitahmzalendo.kejafy.payment.dto.request.InitiateMpesaPaymentRequest;
import com.pitahmzalendo.kejafy.payment.dto.response.*;
import com.pitahmzalendo.kejafy.payment.config.StripeProperties;
import com.pitahmzalendo.kejafy.payment.entity.PaymentProvider;
import com.pitahmzalendo.kejafy.payment.entity.PaymentStatus;
import com.pitahmzalendo.kejafy.payment.entity.PaymentTransaction;
import com.pitahmzalendo.kejafy.payment.exception.PaymentNotFoundException;
import com.pitahmzalendo.kejafy.payment.mpesa.MpesaClient;
import com.pitahmzalendo.kejafy.payment.pricing.SubscriptionPricing;
import com.pitahmzalendo.kejafy.payment.repository.PaymentTransactionRepository;
import com.pitahmzalendo.kejafy.payment.stripe.StripeClient;
import com.pitahmzalendo.kejafy.subscription.service.SubscriptionService;
import com.pitahmzalendo.kejafy.user.entity.User;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Single orchestration point for both payment providers. Neither
 * MpesaClient nor StripeClient nor SubscriptionService talk to each
 * other directly — everything routes through here, so "what happens
 * when a payment succeeds" (mark the transaction, extend the
 * subscription) lives in exactly one place regardless of which
 * provider triggered it.
 */
@Service
@Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    // How long a PENDING M-Pesa transaction can sit before we consider
    // it abandoned if no callback arrives — see PaymentReconciliationService.
    static final long PENDING_TIMEOUT_MINUTES = 10;

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionPricing subscriptionPricing;
    private final MpesaClient mpesaClient;
    private final StripeClient stripeClient;
    private final StripeProperties stripeProperties;

    public PaymentService(PaymentTransactionRepository paymentTransactionRepository,
                          SubscriptionService subscriptionService,
                          SubscriptionPricing subscriptionPricing,
                          MpesaClient mpesaClient,
                          StripeClient stripeClient,
                          StripeProperties stripeProperties) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.subscriptionService = subscriptionService;
        this.subscriptionPricing = subscriptionPricing;
        this.mpesaClient = mpesaClient;
        this.stripeClient = stripeClient;
        this.stripeProperties = stripeProperties;
    }

    // ---------------------------------------------------------------
    // M-Pesa
    // ---------------------------------------------------------------

    public MpesaPaymentInitiationResponse initiateMpesaPayment(User user, InitiateMpesaPaymentRequest request) {
        var amount = subscriptionPricing.amountFor(request.plan());
        var currency = subscriptionPricing.getCurrency();

        PaymentTransaction transaction = new PaymentTransaction(
                user, request.plan(), PaymentProvider.MPESA, amount, currency, request.phoneNumber());
        transaction = paymentTransactionRepository.save(transaction);

        StkPushResponse stkResponse = mpesaClient.stkPush(
                request.phoneNumber(),
                amount,
                accountReferenceFor(user),
                "KejaFy " + request.plan().name() + " subscription"
        );

        transaction.attachProviderReferences(stkResponse.checkoutRequestId(), stkResponse.merchantRequestId());
        // No explicit save needed — 'transaction' is a managed entity
        // within this @Transactional method, so the field change is
        // flushed automatically at commit.

        return MpesaPaymentInitiationResponse.from(transaction, stkResponse.customerMessage());
    }

    /**
     * Called by MpesaCallbackController when Safaricom posts the result
     * of an STK push. Daraja callbacks aren't signed, so this treats the
     * callback purely as a trigger to re-check status with Daraja
     * directly (queryStkStatus) rather than trusting the POST body's
     * ResultCode outright — a spoofed callback to this endpoint can't
     * mark a payment as paid on its own.
     */
    public void handleMpesaCallback(MpesaCallbackPayload payload) {
        MpesaCallbackPayload.StkCallback callback = payload.body().stkCallback();
        String checkoutRequestId = callback.checkoutRequestId();

        PaymentTransaction transaction = paymentTransactionRepository
                .findByProviderReference(checkoutRequestId)
                .orElse(null);

        if (transaction == null) {
            log.warn("Received M-Pesa callback for unknown CheckoutRequestID={}", checkoutRequestId);
            return;
        }

        if (transaction.isTerminal()) {
            return; // already resolved — duplicate delivery, ignore
        }

        StkStatusQueryResponse confirmed = mpesaClient.queryStkStatus(checkoutRequestId);

        if (confirmed.paid()) {
            String receipt = callback.mpesaReceiptNumber().orElse(checkoutRequestId);
            transaction.markSucceeded(receipt);
            subscriptionService.activateOrExtend(transaction.getUser(), transaction.getPlan());
            log.info("M-Pesa payment succeeded for transactionId={} receipt={}", transaction.getId(), receipt);
        } else if (confirmed.stillPending()) {
            // Query came back inconclusive (rare) — leave PENDING; the
            // reconciliation sweep will retry or eventually expire it.
            log.info("M-Pesa status query inconclusive for transactionId={}, leaving PENDING",
                    transaction.getId());
        } else {
            transaction.markFailed(callback.resultDesc());
            log.info("M-Pesa payment failed for transactionId={}: {}", transaction.getId(), callback.resultDesc());
        }
    }

    // ---------------------------------------------------------------
    // Stripe (card)
    // ---------------------------------------------------------------

    public CardPaymentInitiationResponse initiateCardPayment(User user, InitiateCardPaymentRequest request) {
        var amount = subscriptionPricing.amountFor(request.plan());
        var currency = subscriptionPricing.getCurrency();

        PaymentTransaction transaction = new PaymentTransaction(
                user, request.plan(), PaymentProvider.STRIPE, amount, currency, null);
        transaction = paymentTransactionRepository.save(transaction);

        PaymentIntent intent = stripeClient.createPaymentIntent(amount, currency, Map.of(
                "paymentTransactionId", transaction.getId().toString(),
                "userId", user.getId().toString(),
                "plan", request.plan().name()
        ));

        transaction.attachProviderReferences(intent.getId(), null);

        return CardPaymentInitiationResponse.from(transaction, intent.getClientSecret(),
                stripeProperties.getPublishableKey());
    }

    /**
     * Called by StripeWebhookController after signature verification
     * (see StripeClient#parseWebhookEvent) — unlike M-Pesa, Stripe
     * signs its webhooks, so a valid signature is enough to trust the
     * event without a secondary confirmation call.
     */
    public void handleStripeWebhook(Event event) {
        if (!"payment_intent.succeeded".equals(event.getType())
                && !"payment_intent.payment_failed".equals(event.getType())) {
            return; // not a transition we act on
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isEmpty()) {
            log.warn("Could not deserialize Stripe event {} (possible API version mismatch)", event.getId());
            return;
        }

        Object stripeObject = deserializer.getObject().get();
        if (!(stripeObject instanceof PaymentIntent intent)) {
            return;
        }

        PaymentTransaction transaction = paymentTransactionRepository
                .findByProviderReference(intent.getId())
                .orElse(null);

        if (transaction == null) {
            log.warn("Received Stripe event for unknown PaymentIntent id={}", intent.getId());
            return;
        }

        if (transaction.isTerminal()) {
            return; // already resolved — Stripe redelivered the event
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            String receipt = intent.getLatestCharge() != null ? intent.getLatestCharge() : intent.getId();
            transaction.markSucceeded(receipt);
            subscriptionService.activateOrExtend(transaction.getUser(), transaction.getPlan());
            log.info("Stripe payment succeeded for transactionId={} receipt={}", transaction.getId(), receipt);
        } else {
            String reason = intent.getLastPaymentError() != null
                    ? intent.getLastPaymentError().getMessage()
                    : "Card payment failed.";
            transaction.markFailed(reason);
            log.info("Stripe payment failed for transactionId={}: {}", transaction.getId(), reason);
        }
    }

    // ---------------------------------------------------------------
    // Shared
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public PaymentTransaction getTransaction(Long transactionId, User viewer) {
        return paymentTransactionRepository.findByIdAndUser(transactionId, viewer)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found."));
    }

    /** Used by PaymentReconciliationService's scheduled sweep. */
    @Transactional(readOnly = true)
    public List<PaymentTransaction> findStalePending() {
        Instant cutoff = Instant.now().minusSeconds(PENDING_TIMEOUT_MINUTES * 60);
        return paymentTransactionRepository.findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, cutoff);
    }

    /**
     * Re-checks a single stale-PENDING transaction directly with its
     * provider and resolves it — called per-row by
     * PaymentReconciliationService rather than in bulk, so one bad
     * provider call doesn't roll back progress on the rest of the sweep.
     */
    public void reconcile(Long transactionId) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId).orElse(null);
        if (transaction == null || transaction.isTerminal()) {
            return;
        }

        if (transaction.getProvider() == PaymentProvider.MPESA) {
            reconcileMpesa(transaction);
        } else {
            reconcileStripe(transaction);
        }
    }

    private void reconcileMpesa(PaymentTransaction transaction) {
        if (transaction.getProviderReference() == null) {
            transaction.markExpired(); // STK push itself never got a CheckoutRequestID
            return;
        }
        StkStatusQueryResponse status = mpesaClient.queryStkStatus(transaction.getProviderReference());
        if (status.paid()) {
            transaction.markSucceeded(transaction.getProviderReference());
            subscriptionService.activateOrExtend(transaction.getUser(), transaction.getPlan());
        } else if (!status.stillPending()) {
            transaction.markFailed(status.resultDesc());
        } else {
            transaction.markExpired();
        }
    }

    private void reconcileStripe(PaymentTransaction transaction) {
        if (transaction.getProviderReference() == null) {
            transaction.markExpired();
            return;
        }
        PaymentIntent intent = stripeClient.retrievePaymentIntent(transaction.getProviderReference());
        if ("succeeded".equals(intent.getStatus())) {
            String receipt = intent.getLatestCharge() != null ? intent.getLatestCharge() : intent.getId();
            transaction.markSucceeded(receipt);
            subscriptionService.activateOrExtend(transaction.getUser(), transaction.getPlan());
        } else if ("canceled".equals(intent.getStatus())) {
            transaction.markFailed("Card payment was canceled.");
        } else {
            transaction.markExpired();
        }
    }

    private String accountReferenceFor(User user) {
        // Shown on the customer's M-Pesa prompt/statement — keep it
        // short and recognisable rather than a raw user id.
        return "KEJAFY-" + user.getId();
    }
}