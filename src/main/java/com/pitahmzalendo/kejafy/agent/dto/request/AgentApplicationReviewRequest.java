package com.pitahmzalendo.kejafy.agent.dto.request;


import jakarta.validation.constraints.Size;

/**
 * reason is only used (and only makes sense) on rejection — the service
 * layer ignores it for approvals.
 */
public record AgentApplicationReviewRequest(
        @Size(max = 500, message = "reason is too long")
        String reason
) {
}
