package com.pitahmzalendo.kejafy.user.service;


import com.pitahmzalendo.kejafy.user.dto.request.SocialSignupRequest;
import com.pitahmzalendo.kejafy.user.dto.request.UpdateProfileRequest;
import com.pitahmzalendo.kejafy.user.dto.response.SocialSignupResult;
import com.pitahmzalendo.kejafy.user.entity.SocialAccount;
import com.pitahmzalendo.kejafy.user.entity.User;
import com.pitahmzalendo.kejafy.user.entity.UserRole;
import com.pitahmzalendo.kejafy.user.exception.AccountLinkingException;
import com.pitahmzalendo.kejafy.user.repository.SocialAccountRepository;
import com.pitahmzalendo.kejafy.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    public UserService(
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository
    ) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
    }

    /**
     * Signs a user up (or logs them in) via a social provider.
     *
     * NOTE: request.emailVerified() must reflect whether the OAuth
     * provider itself confirmed the email address (e.g. Google's
     * "email_verified" claim). Do not default this to true — if the
     * DTO doesn't carry it yet, add it before wiring this in.
     */
    public SocialSignupResult signupWithSocial(SocialSignupRequest request) {
        // 1. If this exact social identity is already linked, this is a login.
        Optional<SocialAccount> existingSocialAccount =
                socialAccountRepository.findByProviderAndProviderUserId(
                        request.provider(),
                        request.providerUserId()
                );

        if (existingSocialAccount.isPresent()) {
            User user = existingSocialAccount.get().getUser();
            log.info("Social login: existing {} account matched, userId={}",
                    request.provider(), user.getId());
            return new SocialSignupResult(user, false);
        }

        // 2. No link yet — find-or-create the underlying user, then link it.
        boolean[] created = {false};
        User user = findOrCreateUser(request, created);

        linkSocialAccount(user, request);

        return new SocialSignupResult(user, created[0]);
    }

    /**
     * Applies a partial update to the given user's own profile fields.
     * Null fields in the request are left untouched — this is a PATCH,
     * not a PUT. Email and role are intentionally not editable here:
     * email changes need re-verification, role changes (e.g. becoming
     * an agent) are a deliberate action with their own workflow, not a
     * profile edit.
     */
    public User updateProfile(User user, UpdateProfileRequest request) {
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        User saved = userRepository.save(user);
        log.info("Profile updated, userId={}", saved.getId());
        return saved;
    }

    private User findOrCreateUser(SocialSignupRequest request, boolean[] created) {
        Optional<User> existingUser = userRepository.findByEmail(request.email());

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // Guard against account takeover: only auto-link to an
            // existing account if the provider actually verified this
            // email address belongs to whoever is signing in.
            if (!request.emailVerified()) {
                log.warn("Blocked social signup linking attempt: provider={} " +
                                "claims unverified email matching existing userId={}",
                        request.provider(), user.getId());
                throw new AccountLinkingException(
                        "This email is already registered. Please verify your " +
                                "email with the provider, or sign in with your password " +
                                "and link this account from settings."
                );
            }

            if (!user.isActive()) {
                log.warn("Social signup attempted against inactive userId={}", user.getId());
                throw new AccountLinkingException(
                        "This account is deactivated. Please contact support."
                );
            }

            return user;
        }

        User newUser = new User();
        newUser.setFirstName(request.firstName());
        newUser.setLastName(request.lastName());
        newUser.setEmail(request.email());
        newUser.setRole(UserRole.CUSTOMER);
        newUser.setActive(true);
        newUser.setVerified(request.emailVerified());

        try {
            newUser = userRepository.save(newUser);
        } catch (DataIntegrityViolationException e) {
            // Lost a race with another signup for the same email.
            newUser = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> e);
            log.info("Recovered from race condition on user creation for email-matched signup");
            created[0] = false;
            return newUser;
        }

        created[0] = true;
        log.info("Created new user via social signup, provider={}, userId={}",
                request.provider(), newUser.getId());
        return newUser;
    }

    private void linkSocialAccount(User user, SocialSignupRequest request) {
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.setUser(user);
        socialAccount.setProvider(request.provider());
        socialAccount.setProviderUserId(request.providerUserId());
        socialAccount.setEmail(request.email());

        try {
            socialAccountRepository.save(socialAccount);
        } catch (DataIntegrityViolationException e) {
            // Another request linked the same (provider, providerUserId)
            // first. Treat this as a login rather than failing the request.
            log.info("Recovered from race condition on social account linking, provider={}",
                    request.provider());
            socialAccountRepository
                    .findByProviderAndProviderUserId(request.provider(), request.providerUserId())
                    .orElseThrow(() -> e);
        }
    }
}