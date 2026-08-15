package com.pitahmzalendo.kejafy.subscription.repository;


import com.pitahmzalendo.kejafy.subscription.entity.Subscription;
import com.pitahmzalendo.kejafy.subscription.entity.SubscriptionStatus;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findFirstByUserAndStatusOrderByExpiresAtDesc(
            User user, SubscriptionStatus status
    );
}
