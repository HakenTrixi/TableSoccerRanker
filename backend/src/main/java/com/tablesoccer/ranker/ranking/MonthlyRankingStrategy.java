package com.tablesoccer.ranker.ranking;

import com.tablesoccer.ranker.match.Match;
import com.tablesoccer.ranker.user.User;

import java.time.YearMonth;
import java.util.List;

public sealed interface MonthlyRankingStrategy
    permits MonthlyEloGainStrategy, MonthlyGoalsScoredStrategy {

    List<PlayerRanking> calculateRankings(List<User> players, List<Match> matches, YearMonth month);

    MonthlyAlgorithm algorithm();
}
