package com.pitahmzalendo.kejafy.config.security;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        String errorCode = "login_failed";

        if (exception instanceof OAuth2AuthenticationException oauthEx
                && oauthEx.getError() != null
                && oauthEx.getError().getErrorCode() != null) {
            errorCode = oauthEx.getError().getErrorCode();
        }

        log.warn("OAuth2 login failed, errorCode={}, message={}", errorCode, exception.getMessage());

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/auth/error")
                .queryParam("error", errorCode)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
