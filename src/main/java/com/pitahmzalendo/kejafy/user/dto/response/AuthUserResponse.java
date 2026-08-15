package com.pitahmzalendo.kejafy.user.dto.response;


import com.pitahmzalendo.kejafy.user.entity.User;
import com.pitahmzalendo.kejafy.user.entity.UserRole;

public record AuthUserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        UserRole role
) {
    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }
}
