package com.pitahmzalendo.kejafy.listing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "body is required")
        @Size(max = 1000, message = "body is too long")
        String body,

        // Omit for a top-level comment. Must reference a top-level
        // comment on the same listing — see CommentService for why
        // replies aren't nested more than one level.
        Long parentCommentId
) {
}
