package com.pitahmzalendo.kejafy.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
// Required for @PreAuthorize("hasRole('AGENT')") / hasRole('ADMIN') on
// AgentApplicationController's admin endpoints, ListingController,
// AdminListingController, AdminAgentApplicationController, etc. Without
// this, those annotations are silently ignored — the endpoint stays
// reachable by anyone authenticated, regardless of role.
@EnableMethodSecurity
public class SecurityConfig {

    // Set app.cors.allowed-origins in application.properties, e.g.
    // app.cors.allowed-origins=http://localhost:5173,https://app.kejafy.com
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    private final CustomOidcUserService customOidcUserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    public SecurityConfig(
            CustomOidcUserService customOidcUserService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            OAuth2LoginFailureHandler oAuth2LoginFailureHandler,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint
    ) {
        this.customOidcUserService = customOidcUserService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless JSON API assumption: no server-rendered forms consuming
                // the session cookie, so CSRF protection isn't providing value here.
                // REVISIT if you add any cookie-authenticated, browser-form-submitted
                // endpoints later (e.g. server-side admin UI) — those would need
                // CSRF re-enabled with CookieCsrfTokenRepository for that path only.
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(contentTypeOptions -> {})
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/error",
                                "/actuator/health", "/actuator/info",
                                "/oauth2/**", "/login/**",
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml",
                                // Safaricom and Stripe hit these directly — neither can carry
                                // our session cookie or OAuth login. Trust is established
                                // differently for each: see MpesaCallbackController (re-query
                                // Daraja rather than trusting the POST body) and
                                // StripeWebhookController (signature verification).
                                "/api/v1/payments/mpesa/callback",
                                "/api/v1/payments/stripe/webhook"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler)
                )

                .exceptionHandling(exceptions -> exceptions
                        // Prevents unauthenticated API/mobile calls from getting a
                        // 302 redirect to Google's login page — they get a clean
                        // 401 JSON body instead. Browser-based oauth2Login flows
                        // are unaffected since they hit /oauth2/authorization/google
                        // directly rather than relying on this entry point.
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}