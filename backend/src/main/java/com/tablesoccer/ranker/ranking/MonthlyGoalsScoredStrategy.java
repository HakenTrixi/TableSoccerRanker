package com.tablesoccer.ranker.ranking;

import com.tablesoccer.ranker.match.Match;
import com.tablesoccer.ranker.match.MatchPlayer;
import com.tablesoccer.ranker.user.User;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public final class MonthlyGoalsScoredStrategy implements MonthlyRankingStrategy {

    @Override
    public List<PlayerRanking> calculateRankings(List<User> players, List<Match> matches, YearMonth month) {
        Map<UUID, Integer> totalGoals = new HashMap<>();

        for (Match match : matches) {
            for (MatchPlayer mp : match.getPlayers()) {
                int scored = match.scoreFor(mp.getTeamColor());
                totalGoals.merge(mp.getUser().getId(), scored, Integer::sum);
            }
        }

        var rank = new AtomicInteger(1);
        return players.stream()
            .filter(p -> totalGoals.containsKey(p.getId()))
            .map(p -> new PlayerRanking(0, p.getId(), p.getDisplayName(), p.getAvatarUrl(),
                totalGoals.getOrDefault(p.getId(), 0)))
            .sorted(Comparator.comparingDouble(PlayerRanking::score).reversed())
            .map(pr -> new PlayerRanking(rank.getAndIncrement(), pr.userId(), pr.displayName(), pr.avatarUrl(), pr.score()))
            .toList();
    }

    @Override
    public MonthlyAlgorithm algorithm() {
        return MonthlyAlgorithm.MONTHLY_GOALS_SCORED;
    }
}
