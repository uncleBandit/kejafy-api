package com.pitahmzalendo.kejafy.subscription.service;

import com.pitahmzalendo.kejafy.subscription.entity.Subscription;
import com.pitahmzalendo.kejafy.subscription.entity.SubscriptionStatus;
import com.pitahmzalendo.kejafy.subscription.repository.SubscriptionRepository;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}