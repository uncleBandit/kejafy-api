package com.pitahmzalendo.kejafy.listing.entity;

import com.pitahmzalendo.kejafy.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A comment on a listing. Reading is public (see CommentController /
 * CommentService.getComments) — the paywall in this product is on
 * location/detail and on posting, not on reading the conversation.
 * Posting requires either an active subscription or being the listing's
 * own agent (an agent must always be able to answer questions on their
 * own listing) — see CommentService.addComment.
 *
 * One level of replies only (parent must itself be a top-level comment):
 * enough for "agent answers a question" without building a full nested
 * thread UI.
 */
@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comments_listing", columnList = "listing_id"),
                @Index(name = "idx_comments_parent", columnList = "parent_comment_id")
        }
)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_comment_listing"))
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_comment_author"))
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id",
            foreignKey = @ForeignKey(name = "fk_comment_parent"))
    private Comment parentComment;

    @Column(nullable = false, length = 1000)
    private String body;

    // Soft delete: keeps replies attached to a removed top-level comment
    // intact instead of orphaning them, and preserves moderation history.
    // CommentService filters these out of normal reads.
    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    protected Comment() {
        // JPA
    }

    public Comment(Listing listing, User author, String body, Comment parentComment) {
        this.listing = listing;
        this.author = author;
        this.body = body;
        this.parentComment = parentComment;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deleted = true;
        this.body = "[deleted]";
    }

    public boolean isTopLevel() {
        return parentComment == null;
    }

    public boolean isAuthoredBy(User user) {
        return author != null && author.getId() != null && author.getId().equals(user.getId());
    }

    public Long getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public User getAuthor() {
        return author;
    }

    public Comment getParentComment() {
        return parentComment;
    }

    public String getBody() {
        return body;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
