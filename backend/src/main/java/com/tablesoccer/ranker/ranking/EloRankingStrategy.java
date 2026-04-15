package com.tablesoccer.ranker.ranking;

import com.tablesoccer.ranker.match.Match;
import com.tablesoccer.ranker.match.MatchPlayer;
import com.tablesoccer.ranker.match.TeamColor;
import com.tablesoccer.ranker.user.User;
import com.tablesoccer.ranker.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public final class EloRankingStrategy implements LongTermRankingStrategy {

    private static final int K = 32;
    private static final double WIN_BONUS = 0.2;
    private static final double S_BASE = 0.5;
    private static final int GOAL_RATIO_DIVISOR = 20;
    private static final int ELO_DIFF_SCALING = 400;
    private static final int DEFAULT_ELO = 1000;

    private final UserRepository userRepository;

    public EloRankingStrategy(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<PlayerRanking> calculateRankings(List<User> players, List<Match> matches) {
        Map<UUID, Integer> ratings = new HashMap<>();
        Map<UUID, User> playersById = new HashMap<>();
        players.forEach(p -> {
            ratings.put(p.getId(), DEFAULT_ELO);
            playersById.put(p.getId(), p);
        });

        for (Match match : matches) {
            applyMatch(match, ratings);
        }

        var rank = new AtomicInteger(1);
        return ratings.entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .map(entry -> {
                User user = playersById.get(entry.getKey());
                return new PlayerRanking(
                    rank.getAndIncrement(),
                    user.getId(),
                    user.getDisplayName(),
                    user.getAvatarUrl(),
                    entry.getValue()
                );
            })
            .toList();
    }

    @Override
    public void updateRatingsAfterMatch(Match match) {
        Map<UUID, Integer> ratings = new HashMap<>();
        for (MatchPlayer mp : match.getPlayers()) {
            ratings.put(mp.getUser().getId(), mp.getUser().getEloRating());
        }

        applyMatch(match, ratings);

        for (MatchPlayer mp : match.getPlayers()) {
            User user = mp.getUser();
            user.setEloRating(ratings.get(user.getId()));
            userRepository.save(user);
        }
    }

    @Override
    public void previewMatch(Match match) {
        Map<UUID, Integer> ratings = new HashMap<>();
        for (MatchPlayer mp : match.getPlayers()) {
            ratings.put(mp.getUser().getId(), mp.getUser().getEloRating());
        }
        applyMatch(match, ratings);
    }

    @Override
    public LongTermAlgorithm algorithm() {
        return LongTermAlgorithm.ELO;
    }

    private void applyMatch(Match match, Map<UUID, Integer> ratings) {
        List<MatchPlayer> yellowPlayers = match.getPlayers().stream()
            .filter(mp -> mp.getTeamColor() == TeamColor.YELLOW).toList();
        List<MatchPlayer> whitePlayers = match.getPlayers().stream()
            .filter(mp -> mp.getTeamColor() == TeamColor.WHITE).toList();

        double yellowTeamElo = yellowPlayers.stream()
            .mapToInt(mp -> ratings.getOrDefault(mp.getUser().getId(), DEFAULT_ELO)).average().orElse(DEFAULT_ELO);
        double whiteTeamElo = whitePlayers.stream()
            .mapToInt(mp -> ratings.getOrDefault(mp.getUser().getId(), DEFAULT_ELO)).average().orElse(DEFAULT_ELO);

        TeamColor winner = match.winnerColor();

        for (MatchPlayer mp : yellowPlayers) {
            double s = calculateScore(match.getYellowScore(), match.getWhiteScore(),
                winner == TeamColor.YELLOW);
            double p = expectedProbability(yellowTeamElo, whiteTeamElo);
            int oldElo = ratings.getOrDefault(mp.getUser().getId(), DEFAULT_ELO);
            int newElo = (int) Math.round(oldElo + K * (s - p));
            ratings.put(mp.getUser().getId(), newElo);

            mp.setEloBefore(oldElo);
            mp.setEloAfter(newElo);
            mp.setEloChange(newElo - oldElo);
            mp.setTeamElo(yellowTeamElo);
            mp.setWinProbability(Math.round(p * 10000.0) / 100.0); // percentage with 2 decimals
        }

        for (MatchPlayer mp : whitePlayers) {
            double s = calculateScore(match.getWhiteScore(), match.getYellowScore(),
                winner == TeamColor.WHITE);
            double p = expectedProbability(whiteTeamElo, yellowTeamElo);
            int oldElo = ratings.getOrDefault(mp.getUser().getId(), DEFAULT_ELO);
            int newElo = (int) Math.round(oldElo + K * (s - p));
            ratings.put(mp.getUser().getId(), newElo);

            mp.setEloBefore(oldElo);
            mp.setEloAfter(newElo);
            mp.setEloChange(newElo - oldElo);
            mp.setTeamElo(whiteTeamElo);
            mp.setWinProbability(Math.round(p * 10000.0) / 100.0);
        }
    }

    private double calculateScore(int goalsScored, int goalsConceded, boolean isWinner) {
        double winBonus = isWinner ? WIN_BONUS : 0;
        return S_BASE + winBonus + (double) (goalsScored - goalsConceded) / GOAL_RATIO_DIVISOR;
    }

    private double expectedProbability(double myTeamElo, double opponentTeamElo) {
        return 1.0 / (1.0 + Math.pow(10, (opponentTeamElo - myTeamElo) / ELO_DIFF_SCALING));
    }
}
