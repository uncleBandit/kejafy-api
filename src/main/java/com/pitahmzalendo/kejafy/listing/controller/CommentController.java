package com.pitahmzalendo.kejafy.listing.controller;


import com.pitahmzalendo.kejafy.config.security.KejafyOidcUser;
import com.pitahmzalendo.kejafy.listing.dto.request.CreateCommentRequest;
import com.pitahmzalendo.kejafy.listing.dto.response.CommentResponse;
import com.pitahmzalendo.kejafy.listing.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Reading is open to any authenticated user regardless of subscription
 * (see CommentService.getComments) — posting is gated inside
 * CommentService.addComment, not here, so there's exactly one place
 * that decides who can write a comment.
 */
@RestController
@RequestMapping("/api/v1/listings/{listingId}/comments")
@Tag(name = "Comments", description = "Public reads; posting requires an active subscription or listing ownership")
@SecurityRequirement(name = "sessionCookie")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(summary = "Read a listing's comments, oldest first — open to everyone, subscribed or not")
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable Long listingId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<CommentResponse> page = commentService.getComments(listingId, pageable)
                .map(CommentResponse::from);
        return ResponseEntity.ok(page);
    }

    @PostMapping
    @Operation(
            summary = "Post a comment or reply",
            description = "Requires an active subscription, unless the caller is the listing's own agent or an admin."
    )
    @ApiResponse(responseCode = "201", description = "Comment posted")
    @ApiResponse(responseCode = "403", description = "No active subscription")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long listingId,
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        var comment = commentService.addComment(listingId, principal.getDomainUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(comment));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment — the comment's author or an admin only")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long listingId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal KejafyOidcUser principal
    ) {
        commentService.deleteComment(commentId, principal.getDomainUser());
        return ResponseEntity.noContent().build();
    }
}
