package com.pitahmzalendo.kejafy.chat.entity;

import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A single thread between one CUSTOMER and one AGENT — reused across
 * every listing they discuss, the way a WhatsApp business chat works,
 * rather than spawning a new thread per listing. {@code originListing}
 * just records what the customer was looking at when they first reached
 * out, for display context; it's not a constraint on what gets discussed.
 *
 * Starting a conversation is subscription-gated (see ChatService) —
 * chatting is the other half of the subscriber value prop alongside
 * exact location. Once started, an agent can always reply even if the
 * customer's subscription later lapses; only the customer's ability to
 * send new messages is re-checked live.
 */
@Entity
@Table(
        name = "conversations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_conversation_customer_agent", columnNames = {"customer_id", "agent_id"})
        },
        indexes = {
                @Index(name = "idx_conversations_customer", columnList = "customer_id"),
                @Index(name = "idx_conversations_agent", columnList = "agent_id")
        }
)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_conversation_customer"))
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_conversation_agent"))
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_listing_id",
            foreignKey = @ForeignKey(name = "fk_conversation_origin_listing"))
    private Listing originListing;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Denormalized for cheap "most recent conversation first" sorting in
    // the inbox list, without joining/aggregating over messages.
    @Column(name = "last_message_at", nullable = false)
    private LocalDateTime lastMessageAt;

    protected Conversation() {
        // JPA
    }

    public Conversation(User customer, User agent, Listing originListing) {
        this.customer = customer;
        this.agent = agent;
        this.originListing = originListing;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastMessageAt = createdAt;
    }

    public void touch() {
        this.lastMessageAt = LocalDateTime.now();
    }

    public boolean isParticipant(User user) {
        return isSameUser(customer, user) || isSameUser(agent, user);
    }

    public User theOtherParticipant(User user) {
        return isSameUser(customer, user) ? agent : customer;
    }

    private boolean isSameUser(User a, User b) {
        return a != null && a.getId() != null && a.getId().equals(b.getId());
    }

    public Long getId() {
        return id;
    }

    public User getCustomer() {
        return customer;
    }

    public User getAgent() {
        return agent;
    }

    public Listing getOriginListing() {
        return originListing;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }
}
