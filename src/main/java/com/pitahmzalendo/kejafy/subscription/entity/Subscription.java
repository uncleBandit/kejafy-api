package com.pitahmzalendo.kejafy.subscription.entity;


import com.pitahmzalendo.kejafy.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Deliberately minimal for now: fields to represent "does this user
 * currently have paid access," nothing about billing/payment provider
 * integration yet (Stripe/M-Pesa/etc — that's a separate decision).
 *
 * NOT yet referenced by any authorization logic. Once a property/listing
 * feature exists with location data to gate, the natural place to check
 * this is a method like SubscriptionService.hasActiveSubscription(user) —
 * called from whatever controller/service serves listing details, not
 * duplicated inline wherever a location might be shown.
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Subscription() {
        // JPA
    }

    public Subscription(User user, SubscriptionPlan plan, Instant startsAt, Instant expiresAt) {
        this.user = user;
        this.plan = plan;
        this.status = SubscriptionStatus.ACTIVE;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
    }

    /** True if this subscription is currently active and not past its expiry. */
    public boolean isCurrentlyActive() {
        return status == SubscriptionStatus.ACTIVE && Instant.now().isBefore(expiresAt);
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

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
