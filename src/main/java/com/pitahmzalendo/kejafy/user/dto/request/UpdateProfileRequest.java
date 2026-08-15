package com.pitahmzalendo.kejafy.user.dto.request;


import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * All fields optional — this is a partial update (PATCH semantics).
 * Whatever the client omits (sends as null) is left unchanged by
 * UserService.updateProfile. Email and role are deliberately not
 * present here; see UserService for why.
 */
public record UpdateProfileRequest(
        @Size(min = 1, max = 100, message = "firstName must be 1-100 characters")
        String firstName,

        @Size(min = 1, max = 100, message = "lastName must be 1-100 characters")
        String lastName,

        // Loose E.164-ish check: optional leading +, 7-15 digits. Adjust
        // if you need stricter validation for a specific country format.
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "phoneNumber must be a valid phone number")
        String phoneNumber,

        // Expected to be a URL from your object storage (S3/Cloudinary/etc),
        // not raw file bytes — the client uploads the image separately and
        // sends back the resulting URL.
        @Size(max = 2048, message = "avatarUrl is too long")
        String avatarUrl
) {
}
