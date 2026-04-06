package com.tablesoccer.ranker.match;

import com.tablesoccer.ranker.user.User;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "match_players",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"match_id", "user_id"}),
           @UniqueConstraint(columnNames = {"match_id", "team_color", "player_role"})
       })
public class MatchPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_color", nullable = false, length = 10)
    private TeamColor teamColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "player_role", nullable = false, length = 10)
    private PlayerRole playerRole;

    @Column(name = "elo_before")
    private Integer eloBefore;

    @Column(name = "elo_after")
    private Integer eloAfter;

    @Column(name = "elo_change")
    private Integer eloChange;

    @Column(name = "team_elo")
    private Double teamElo;

    @Column(name = "win_probability")
    private Double winProbability;

    // Getters and setters

    public UUID getId() { return id; }

    public Match getMatch() { return match; }
    public void setMatch(Match match) { this.match = match; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public TeamColor getTeamColor() { return teamColor; }
    public void setTeamColor(TeamColor teamColor) { this.teamColor = teamColor; }

    public PlayerRole getPlayerRole() { return playerRole; }
    public void setPlayerRole(PlayerRole playerRole) { this.playerRole = playerRole; }

    public Integer getEloBefore() { return eloBefore; }
    public void setEloBefore(Integer eloBefore) { this.eloBefore = eloBefore; }

    public Integer getEloAfter() { return eloAfter; }
    public void setEloAfter(Integer eloAfter) { this.eloAfter = eloAfter; }

    public Integer getEloChange() { return eloChange; }
    public void setEloChange(Integer eloChange) { this.eloChange = eloChange; }

    public Double getTeamElo() { return teamElo; }
    public void setTeamElo(Double teamElo) { this.teamElo = teamElo; }

    public Double getWinProbability() { return winProbability; }
    public void setWinProbability(Double winProbability) { this.winProbability = winProbability; }
}
