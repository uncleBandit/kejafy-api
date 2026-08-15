package com.pitahmzalendo.kejafy.config.security;


import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Wraps the OIDC principal Spring Security builds from Google's response
 * with our own domain User, so controllers can do:
 *
 *   @AuthenticationPrincipal KejafyOidcUser principal
 *   User user = principal.getDomainUser();
 *
 * instead of re-looking the user up by email/subject on every request.
 */
public class KejafyOidcUser implements OidcUser {

    private final OidcUser delegate;
    private final User domainUser;
    private final boolean newlyCreated;

    public KejafyOidcUser(OidcUser delegate, User domainUser, boolean newlyCreated) {
        this.delegate = delegate;
        this.domainUser = domainUser;
        this.newlyCreated = newlyCreated;
    }

    public User getDomainUser() {
        return domainUser;
    }

    /** True if this login just created a brand-new KejaFy account. */
    public boolean isNewlyCreated() {
        return newlyCreated;
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Authorities come from our own domain role, not from Google —
        // Google has no concept of "ADMIN" vs "CUSTOMER" in KejaFy.
        return List.of(new SimpleGrantedAuthority("ROLE_" + domainUser.getRole().name()));
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
}