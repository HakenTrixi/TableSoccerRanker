package com.tablesoccer.ranker.ranking;

import com.tablesoccer.ranker.match.Match;
import com.tablesoccer.ranker.user.User;

import java.util.List;

public sealed interface LongTermRankingStrategy
    permits EloRankingStrategy, AvgGoalDiffRankingStrategy {

    List<PlayerRanking> calculateRankings(List<User> players, List<Match> matches);

    void updateRatingsAfterMatch(Match match);

    /** Preview ELO fields on MatchPlayer without persisting user changes. */
    default void previewMatch(Match match) {}

    LongTermAlgorithm algorithm();
}
