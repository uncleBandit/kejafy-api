package com.pitahmzalendo.kejafy.listing.entity;

import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One row per (user, listing) like — the unique constraint is what makes
 * "double tap to like" idempotent: a repeat like is a no-op, not a second
 * row. Liking is intentionally NOT gated on subscription; it's one of the
 * few things a non-subscriber can do besides watch.
 */
@Entity
@Table(
        name = "listing_likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_listing_like_user", columnNames = {"listing_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_listing_likes_listing", columnList = "listing_id"),
                @Index(name = "idx_listing_likes_user", columnList = "user_id")
        }
)
public class ListingLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_listing_like_listing"))
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_listing_like_user"))
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ListingLike() {
        // JPA
    }

    public ListingLike(Listing listing, User user) {
        this.listing = listing;
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
