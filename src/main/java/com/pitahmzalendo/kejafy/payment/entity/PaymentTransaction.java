package com.pitahmzalendo.kejafy.payment.entity;

import com.pitahmzalendo.kejafy.subscription.entity.SubscriptionPlan;
import com.pitahmzalendo.kejafy.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One record per payment attempt, regardless of provider. This is the
 * source of truth for "did this specific checkout succeed" — separate
 * from Subscription, which only tracks current access. A single
 * Subscription can be extended by many PaymentTransactions over time.
 *
 * providerReference is the provider's handle for the attempt
 * (CheckoutRequestID for M-Pesa, PaymentIntent id for Stripe) — how we
 * match an inbound callback/webhook back to the row that started it.
 * providerReceipt is only set once SUCCEEDED (M-Pesa receipt number /
 * Stripe charge id) — the proof-of-payment reference to show the user
 * or reconcile against provider statements.
 *
 * Both PaymentService callback handlers check status before flipping it
 * (see markSucceeded/markFailed) so a duplicate callback — Safaricom
 * retries, Stripe redelivering an event — is a no-op instead of
 * double-extending a subscription.
 */
@Entity
@Table(
        name = "payment_transactions",
        indexes = {
                @Index(name = "idx_payment_tx_user", columnList = "user_id"),
                @Index(name = "idx_payment_tx_provider_reference", columnList = "provider_reference")
        }
)
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payment_tx_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    // CheckoutRequestID (M-Pesa) or PaymentIntent id (Stripe). Unique so
    // a provider retrying its initiation call can't create two rows for
    // what should be one attempt.
    @Column(name = "provider_reference", unique = true, length = 128)
    private String providerReference;

    // MerchantRequestID — M-Pesa only, kept alongside CheckoutRequestID
    // because Daraja's own support/reconciliation tooling asks for both.
    @Column(name = "provider_merchant_reference", length = 128)
    private String providerMerchantReference;

    // M-Pesa receipt number (e.g. "QGR7XXXXHT") or Stripe charge id.
    // Null until SUCCEEDED.
    @Column(name = "provider_receipt", length = 128)
    private String providerReceipt;

    // M-Pesa only — the MSISDN the STK push was sent to.
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    // Optimistic locking: the provider callback and a reconciliation
    // sweep (see PaymentReconciliationService) could race on the same
    // PENDING row. Whichever loses the race retries or is a no-op — see
    // callers of markSucceeded/markFailed.
    @Version
    private Long version;

    protected PaymentTransaction() {
        // JPA
    }

    public PaymentTransaction(User user, SubscriptionPlan plan, PaymentProvider provider,
                              BigDecimal amount, String currency, String phoneNumber) {
        this.user = user;
        this.plan = plan;
        this.provider = provider;
        this.status = PaymentStatus.PENDING;
        this.amount = amount;
        this.currency = currency;
        this.phoneNumber = phoneNumber;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isTerminal() {
        return status == PaymentStatus.SUCCEEDED
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.EXPIRED;
    }

    public void attachProviderReferences(String providerReference, String providerMerchantReference) {
        this.providerReference = providerReference;
        this.providerMerchantReference = providerMerchantReference;
    }

    public void markSucceeded(String providerReceipt) {
        if (isTerminal()) {
            return; // already resolved — duplicate callback/webhook, ignore
        }
        this.status = PaymentStatus.SUCCEEDED;
        this.providerReceipt = providerReceipt;
    }

    public void markFailed(String reason) {
        if (isTerminal()) {
            return;
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void markExpired() {
        if (isTerminal()) {
            return;
        }
        this.status = PaymentStatus.EXPIRED;
        this.failureReason = "No confirmation received from provider in time.";
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public String getProviderMerchantReference() {
        return providerMerchantReference;
    }

    public String getProviderReceipt() {
        return providerReceipt;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
