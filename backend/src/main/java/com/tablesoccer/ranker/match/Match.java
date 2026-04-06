package com.tablesoccer.ranker.match;

import com.tablesoccer.ranker.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "yellow_score", nullable = false)
    private int yellowScore;

    @Column(name = "white_score", nullable = false)
    private int whiteScore;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_id", nullable = false)
    private User recordedBy;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchPlayer> players = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (playedAt == null) {
            playedAt = Instant.now();
        }
    }

    public void addPlayer(User user, TeamColor teamColor, PlayerRole playerRole) {
        var mp = new MatchPlayer();
        mp.setMatch(this);
        mp.setUser(user);
        mp.setTeamColor(teamColor);
        mp.setPlayerRole(playerRole);
        players.add(mp);
    }

    public TeamColor winnerColor() {
        if (yellowScore > whiteScore) return TeamColor.YELLOW;
        if (whiteScore > yellowScore) return TeamColor.WHITE;
        return null; // draw
    }

    public int scoreFor(TeamColor color) {
        return color == TeamColor.YELLOW ? yellowScore : whiteScore;
    }

    public int scoreAgainst(TeamColor color) {
        return color == TeamColor.YELLOW ? whiteScore : yellowScore;
    }

    // Getters and setters

    public UUID getId() { return id; }

    public int getYellowScore() { return yellowScore; }
    public void setYellowScore(int yellowScore) { this.yellowScore = yellowScore; }

    public int getWhiteScore() { return whiteScore; }
    public void setWhiteScore(int whiteScore) { this.whiteScore = whiteScore; }

    public Instant getPlayedAt() { return playedAt; }
    public void setPlayedAt(Instant playedAt) { this.playedAt = playedAt; }

    public User getRecordedBy() { return recordedBy; }
    public void setRecordedBy(User recordedBy) { this.recordedBy = recordedBy; }

    public List<MatchPlayer> getPlayers() { return players; }

    public Instant getCreatedAt() { return createdAt; }
}
