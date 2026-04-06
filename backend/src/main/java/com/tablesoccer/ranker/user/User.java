package com.tablesoccer.ranker.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "google_sub", unique = true)
    private String googleSub;

    @Column(unique = true, length = 100)
    private String username;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.PLAYER;

    @Column(name = "elo_rating", nullable = false)
    private int eloRating = 1000;

    @Column(name = "attacker_elo", nullable = false)
    private int attackerElo = 1000;

    @Column(name = "defender_elo", nullable = false)
    private int defenderElo = 1000;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and setters

    public UUID getId() { return id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getGoogleSub() { return googleSub; }
    public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public int getEloRating() { return eloRating; }
    public void setEloRating(int eloRating) { this.eloRating = eloRating; }

    public int getAttackerElo() { return attackerElo; }
    public void setAttackerElo(int attackerElo) { this.attackerElo = attackerElo; }

    public int getDefenderElo() { return defenderElo; }
    public void setDefenderElo(int defenderElo) { this.defenderElo = defenderElo; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getVersion() { return version; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
