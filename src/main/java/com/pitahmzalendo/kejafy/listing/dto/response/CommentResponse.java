package com.pitahmzalendo.kejafy.listing.dto.response;

import com.pitahmzalendo.kejafy.listing.entity.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long listingId,
        Long authorId,
        String authorName,
        String authorAvatarUrl,
        Long parentCommentId,
        String body,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getListing().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getFirstName() + " " + comment.getAuthor().getLastName(),
                comment.getAuthor().getAvatarUrl(),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                comment.getBody(),
                comment.isDeleted(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
