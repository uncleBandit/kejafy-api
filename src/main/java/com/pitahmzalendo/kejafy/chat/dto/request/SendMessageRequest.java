package com.pitahmzalendo.kejafy.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "body is required")
        @Size(max = 2000, message = "body is too long")
        String body
) {
}
