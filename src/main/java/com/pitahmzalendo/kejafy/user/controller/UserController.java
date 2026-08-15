package com.pitahmzalendo.kejafy.user.controller;


import com.pitahmzalendo.kejafy.config.security.KejafyOidcUser;
import com.pitahmzalendo.kejafy.user.dto.request.UpdateProfileRequest;
import com.pitahmzalendo.kejafy.user.dto.response.UserProfileResponse;
import com.pitahmzalendo.kejafy.user.entity.User;
import com.pitahmzalendo.kejafy.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service profile endpoints only. Deliberately scoped to "me" —
 * no /users/{id} lookups or admin listing here yet, since KejaFy doesn't
 * have agent/admin role workflows wired up. Add those as a separate
 * controller (or separate authorization rules on these same paths) once
 * that's actually needed, rather than growing this one past its current
 * single responsibility.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Self-service profile management")
@SecurityRequirement(name = "sessionCookie")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the caller's own profile")
    @ApiResponse(responseCode = "200", description = "The caller's profile")
    @ApiResponse(responseCode = "401", description = "No active session")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal KejafyOidcUser principal
    ) {
        return ResponseEntity.ok(UserProfileResponse.from(principal.getDomainUser()));
    }

    @PatchMapping("/me")
    @Operation(
            summary = "Update the caller's own profile",
            description = "Partial update — omit (or send null for) any field you don't " +
                    "want to change. Email and role cannot be changed here."
    )
    @ApiResponse(responseCode = "200", description = "The updated profile")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "No active session")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        User updated = userService.updateProfile(principal.getDomainUser(), request);
        return ResponseEntity.ok(UserProfileResponse.from(updated));
    }
}
