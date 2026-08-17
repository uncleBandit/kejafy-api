package com.pitahmzalendo.kejafy.user.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_phone_number", columnList = "phone_number")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    // Nullable: users who sign up via OAuth only (Google, etc.) never set
    // a password. Only enforce non-null at the service layer, for users
    // who go through local email/password registration (if/when that
    // flow exists) — not at the schema level, since that would break
    // every social-only signup.
    @Column
    private String password;

    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Optimistic locking: cheap protection against lost updates if two
    // requests (e.g. a profile edit and a social-account link) modify
    // the same user concurrently.
    @Version
    private Long version;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    public User() {
        // JPA
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

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<SocialAccount> getSocialAccounts() {
        return socialAccounts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        // Entity equality by id only, and only once persisted — two
        // transient (unsaved) instances are never equal to each other,
        // since neither has an identity yet.
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        // Constant hash code for JPA entities using DB-generated ids:
        // using getId().hashCode() would change the hash after the first
        // save, corrupting any HashSet/HashMap the entity was already in.
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        // Deliberately excludes password, and does not eagerly touch
        // socialAccounts (lazy collection — including it here would
        // trigger a query, or a LazyInitializationException outside a
        // transaction, every time an entity gets logged).
        return "User{id=%s, email=%s, role=%s, active=%s, verified=%s}"
                .formatted(id, email, role, active, verified);
    }
}