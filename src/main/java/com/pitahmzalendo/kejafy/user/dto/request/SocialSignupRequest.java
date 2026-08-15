package com.pitahmzalendo.kejafy.user.dto.request;

import com.pitahmzalendo.kejafy.user.entity.AuthProvider;

public record SocialSignupRequest(
        AuthProvider provider,
        String providerUserId,
        String email,
        boolean emailVerified,
        String firstName,
        String lastName
) {
    public SocialSignupRequest {
        if (provider == null) {
            throw new IllegalArgumentException("provider is required");
        }
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("providerUserId is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        email = email.trim().toLowerCase();
    }
}