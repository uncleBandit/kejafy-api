package com.pitahmzalendo.kejafy.user.controller;


import com.pitahmzalendo.kejafy.config.security.KejafyOidcUser;
import com.pitahmzalendo.kejafy.user.dto.response.AuthUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/v1/auth/success was removed: OAuth2LoginSuccessHandler now redirects
 * the browser straight to the frontend after login, so this controller no
 * longer needs to be a redirect target. What the frontend actually needs
 * post-redirect is a way to fetch the logged-in user's data (the redirect
 * itself only carries a "new account?" flag) and a way to log out.
 *
 * Both endpoints require authentication via the session cookie set during
 * the OAuth2 login flow — SecurityConfig's anyRequest().authenticated()
 * already enforces that; no extra checks needed here.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Current-session identity and logout")
@SecurityRequirement(name = "sessionCookie")
public class AuthController {

    @GetMapping("/me")
    @Operation(
            summary = "Get the currently authenticated user",
            description = "Requires an active session from a completed OAuth2 login " +
                    "(GET /oauth2/authorization/google). Returns 401 if not logged in."
    )
    @ApiResponse(responseCode = "200", description = "The authenticated user")
    @ApiResponse(responseCode = "401", description = "No active session")
    public ResponseEntity<AuthUserResponse> me(@AuthenticationPrincipal KejafyOidcUser principal) {
        return ResponseEntity.ok(AuthUserResponse.from(principal.getDomainUser()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate the current session")
    @ApiResponse(responseCode = "204", description = "Session invalidated")
    public ResponseEntity<Void> logout(HttpServletRequest request) throws Exception {
        // Invalidates the session and clears the SecurityContext. Using the
        // servlet-level logout() here rather than Spring Security's default
        // /logout endpoint keeps this under our own /api/v1/auth namespace
        // and returns a plain 204 instead of a redirect, which is what a
        // fetch()-based frontend logout button actually wants.
        request.logout();
        return ResponseEntity.noContent().build();
    }
}
