package com.tablesoccer.ranker.stats;

import java.util.List;
import java.util.UUID;

public record PlayerStats(
    UUID userId,
    String displayName,
    int totalMatches,
    int wins,
    int losses,
    int draws,
    double winRate,
    int totalGoalsScored,
    int totalGoalsConceded,
    double avgGoalsScoredPerMatch,
    double avgGoalsConcededPerMatch,
    int currentElo,
    int attackerElo,
    int defenderElo,
    Integer highestEloEver,
    Integer lowestEloEver,
    Integer biggestEloGain,
    Integer biggestEloLoss,
    double averageEloChange,
    int longestWinStreak,
    int longestLoseStreak,
    BiggestWin biggestWin,
    StreakInfo currentStreak,
    int bludistakWins,
    PartnerStat bestPartner,
    PartnerStat worstPartner,
    OpponentStat nemesis,
    OpponentStat favoriteOpponent,
    RoleStats attackerStats,
    RoleStats defenderStats,
    ColorStat yellowStats,
    ColorStat whiteStats,
    List<FormEntry> recentForm
) {
    public record PartnerStat(UUID userId, String displayName, int matches, int wins, double winRate) {}
    public record OpponentStat(UUID userId, String displayName, int matches, int losses, double lossRate) {}
    public record RoleStats(int matches, int wins, double winRate, double avgGoalDiff) {}
    public record ColorStat(int matches, int wins, double winRate, int goalsScored, int goalsConceded) {}
    public record FormEntry(UUID matchId, boolean won, int goalDiff) {}
    public record BiggestWin(int goalDiff, String description) {}
    public record StreakInfo(String type, int count) {}
}
