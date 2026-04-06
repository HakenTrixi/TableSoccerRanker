package com.tablesoccer.ranker.stats;

import java.util.List;
import java.util.UUID;

public record CompanyStats(
    long totalMatches,
    long totalGoals,
    PlayerStat mostActivePlayer,
    PlayerStat topScorer,
    MatchStat biggestWin,
    StreakStat longestWinStreak,
    StreakStat longestLoseStreak,
    PairStat mostCommonPairing,
    List<CurrentStreak> currentStreaks,
    BludistakChamp currentBludistak,
    BludistakChamp mostBludistakWins,
    ColorStats colorStats,
    List<MonthlyActivity> monthlyActivity
) {
    public record PlayerStat(UUID userId, String displayName, long value) {}
    public record MatchStat(UUID matchId, int goalDiff, String description) {}
    public record StreakStat(UUID userId, String displayName, int streak) {}
    public record PairStat(UUID player1Id, String player1Name, UUID player2Id, String player2Name, long count) {}
    public record CurrentStreak(UUID userId, String displayName, String type, int count) {}
    public record BludistakChamp(UUID userId, String displayName, int wins, String month) {}
    public record ColorStats(long yellowWins, long whiteWins, double yellowAvgGoals, double whiteAvgGoals) {}
    public record MonthlyActivity(String month, long matchCount) {}
}
