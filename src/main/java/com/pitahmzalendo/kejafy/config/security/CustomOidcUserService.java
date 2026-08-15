package com.pitahmzalendo.kejafy.config.security;


import com.pitahmzalendo.kejafy.user.dto.request.SocialSignupRequest;
import com.pitahmzalendo.kejafy.user.dto.response.SocialSignupResult;
import com.pitahmzalendo.kejafy.user.entity.AuthProvider;
import com.pitahmzalendo.kejafy.user.exception.AccountLinkingException;
import com.pitahmzalendo.kejafy.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Bridges Spring Security's default OIDC user-loading with our own
 * signup/link logic in UserService. This is where a successful Google
 * OAuth handshake actually becomes a KejaFy user record.
 */
@Service
public class CustomOidcUserService extends OidcUserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOidcUserService.class);

    private final UserService userService;

    public CustomOidcUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegates to Spring's default handling first: this is what
        // actually calls Google's userinfo endpoint and validates the
        // ID token. We only step in after that succeeds.
        OidcUser oidcUser = super.loadUser(userRequest);

        AuthProvider provider = resolveProvider(userRequest.getClientRegistration().getRegistrationId());

        String email = oidcUser.getEmail();
        if (email == null || email.isBlank()) {
            // Some providers (notably Apple, and Google accounts with
            // restricted scopes) can omit email. Fail clearly rather
            // than passing a blank email into UserService.
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_available"),
                    "Your " + provider + " account did not share an email address, " +
                            "so we can't create or match your KejaFy account."
            );
        }

        boolean emailVerified = Boolean.TRUE.equals(oidcUser.getEmailVerified());

        SocialSignupRequest signupRequest = new SocialSignupRequest(
                provider,
                oidcUser.getSubject(),
                email,
                emailVerified,
                oidcUser.getGivenName(),
                oidcUser.getFamilyName()
        );

        SocialSignupResult result;
        try {
            result = userService.signupWithSocial(signupRequest);
        } catch (AccountLinkingException e) {
            // Surface as an OAuth2AuthenticationException so it flows through
            // Spring Security's normal failure handling (and our custom
            // OAuth2LoginFailureHandler) instead of becoming a raw 500.
            log.warn("Account linking blocked for provider={}: {}", provider, e.getMessage());
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_linking_conflict", e.getMessage(), null),
                    e.getMessage(),
                    e
            );
        }

        return new KejafyOidcUser(oidcUser, result.user(), result.created());
    }

    private AuthProvider resolveProvider(String registrationId) {
        try {
            return AuthProvider.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"),
                    "Unsupported login provider: " + registrationId
            );
        }
    }
}
