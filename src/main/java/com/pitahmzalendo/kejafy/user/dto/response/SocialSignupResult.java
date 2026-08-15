package com.pitahmzalendo.kejafy.user.dto.response;

import com.pitahmzalendo.kejafy.user.entity.User;

public record SocialSignupResult(
        User user,
        boolean created
) {
}
