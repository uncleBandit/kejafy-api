package com.pitahmzalendo.kejafy.user.repository;

import com.pitahmzalendo.kejafy.user.entity.AuthProvider;
import com.pitahmzalendo.kejafy.user.entity.SocialAccount;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository
        extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(
            AuthProvider provider,
            String providerUserId
    );

    boolean existsByProviderAndProviderUserId(
            AuthProvider provider,
            String providerUserId
    );

    List<SocialAccount> findByUser(User user);
}
