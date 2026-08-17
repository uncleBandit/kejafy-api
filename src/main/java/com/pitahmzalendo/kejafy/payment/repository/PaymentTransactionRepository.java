package com.pitahmzalendo.kejafy.payment.repository;

import com.pitahmzalendo.kejafy.payment.entity.PaymentStatus;
import com.pitahmzalendo.kejafy.payment.entity.PaymentTransaction;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByIdAndUser(Long id, User user);

    Optional<PaymentTransaction> findByProviderReference(String providerReference);

    // Reconciliation sweep target — see PaymentReconciliationService.
    // Anything still PENDING past a cutoff either never got a
    // callback/webhook or the customer abandoned the checkout.
    List<PaymentTransaction> findByStatusAndCreatedAtBefore(PaymentStatus status, Instant cutoff);
}
