package com.pitahmzalendo.kejafy.subscription.service;

import com.pitahmzalendo.kejafy.subscription.entity.Subscription;
import com.pitahmzalendo.kejafy.subscription.entity.SubscriptionPlan;
import com.pitahmzalendo.kejafy.subscription.entity.SubscriptionStatus;
import com.pitahmzalendo.kejafy.subscription.repository.SubscriptionRepository;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Single source of truth for "does this user currently have paid access".
 * Anything that needs to gate content on subscription status (listing
 * location, contact details, DMs, comments) should call
 * {@link #hasActiveSubscription(User)} rather than querying
 * SubscriptionRepository or checking Subscription fields directly —
 * keeps the "what counts as active" rule in one place.
 */
@Service
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public boolean hasActiveSubscription(User user) {
        if (user == null) {
            return false;
        }
        return subscriptionRepository
                .findFirstByUserAndStatusOrderByExpiresAtDesc(user, SubscriptionStatus.ACTIVE)
                .map(Subscription::isCurrentlyActive)
                .orElse(false);
    }

    /**
     * Called by PaymentService once a payment succeeds (M-Pesa callback
     * confirmed via re-query, or a verified Stripe webhook) — never
     * called directly from a controller, since "paid" is the only valid
     * trigger for extending access.
     *
     * If the user already has a currently-active subscription, this
     * extends it from its existing expiry rather than from now — paying
     * early (e.g. renewing a few days before expiry) doesn't lose the
     * remaining time. If there's no active subscription (first purchase,
     * or a lapsed one), it starts fresh from now. Buying a different
     * plan than the one currently active still extends by the newly
     * paid plan's duration on top of the existing expiry, rather than
     * replacing it — a customer switching from MONTHLY to YEARLY keeps
     * the time they already paid for.
     */
    @Transactional
    public Subscription activateOrExtend(User user, SubscriptionPlan plan) {
        Subscription existing = subscriptionRepository
                .findFirstByUserAndStatusOrderByExpiresAtDesc(user, SubscriptionStatus.ACTIVE)
                .filter(Subscription::isCurrentlyActive)
                .orElse(null);

        if (existing != null) {
            existing.setExpiresAt(addPlanDuration(existing.getExpiresAt(), plan));
            return existing;
        }

        Instant now = Instant.now();
        Subscription subscription = new Subscription(user, plan, now, addPlanDuration(now, plan));
        return subscriptionRepository.save(subscription);
    }

    private Instant addPlanDuration(Instant from, SubscriptionPlan plan) {
        Period duration = switch (plan) {
            case MONTHLY -> Period.ofMonths(1);
            case YEARLY -> Period.ofYears(1);
        };
        return ZonedDateTime.ofInstant(from, ZoneOffset.UTC).plus(duration).toInstant();
    }
}