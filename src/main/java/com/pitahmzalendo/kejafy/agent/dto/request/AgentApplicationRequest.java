package com.pitahmzalendo.kejafy.agent.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * idDocumentFrontUrl/idDocumentBackUrl/selfieUrl are URLs from object
 * storage, not raw file bytes — same upload-then-reference pattern as
 * User.avatarUrl.
 */
public record AgentApplicationRequest(
        @NotBlank(message = "idNumber is required")
        @Size(max = 50, message = "idNumber is too long")
        String idNumber,

        @NotBlank(message = "idDocumentFrontUrl is required")
        @Size(max = 2048, message = "idDocumentFrontUrl is too long")
        String idDocumentFrontUrl,

        @Size(max = 2048, message = "idDocumentBackUrl is too long")
        String idDocumentBackUrl,

        @Size(max = 2048, message = "selfieUrl is too long")
        String selfieUrl
) {
}