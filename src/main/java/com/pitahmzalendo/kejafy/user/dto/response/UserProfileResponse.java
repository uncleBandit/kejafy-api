package com.pitahmzalendo.kejafy.user.dto.response;


import com.pitahmzalendo.kejafy.user.entity.User;
import com.pitahmzalendo.kejafy.user.entity.UserRole;

public record UserProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String avatarUrl,
        UserRole role
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getRole()
        );
    }
}