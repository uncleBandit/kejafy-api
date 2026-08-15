package com.pitahmzalendo.kejafy.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * On successful login, redirects back to the frontend rather than a bare
 * API endpoint — a browser just finished a full-page OAuth redirect dance,
 * so it needs somewhere in the SPA to land, not a JSON response.
 *
 * Adjust the target/params to match how your frontend actually wants to
 * pick up the session (e.g. if you issue a short-lived JWT here instead
 * of relying on the session cookie, append it as a fragment, never a
 * query param, to avoid it landing in server logs / browser history).
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        boolean newAccount = authentication.getPrincipal() instanceof KejafyOidcUser kejafyUser
                && kejafyUser.isNewlyCreated();

        log.info("OAuth2 login succeeded, newAccount={}", newAccount);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/auth/callback")
                .queryParam("new", newAccount)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
