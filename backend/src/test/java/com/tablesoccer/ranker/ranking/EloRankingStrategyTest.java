package com.tablesoccer.ranker.ranking;

import com.tablesoccer.ranker.TestDataFactory;
import com.tablesoccer.ranker.match.Match;
import com.tablesoccer.ranker.user.User;
import com.tablesoccer.ranker.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EloRankingStrategyTest {

    @Mock
    UserRepository userRepository;

    EloRankingStrategy strategy;

    User alice, bob, charlie, dave;

    @BeforeEach
    void setUp() {
        strategy = new EloRankingStrategy(userRepository);
        alice = TestDataFactory.createUser("Alice", 1000);
        bob = TestDataFactory.createUser("Bob", 1000);
        charlie = TestDataFactory.createUser("Charlie", 1000);
        dave = TestDataFactory.createUser("Dave", 1000);
    }

    @Test
    void equalTeams_winnerGainsElo() {
        // All start at 1000. Yellow (Alice+Bob) beats White (Charlie+Dave) 10-5
        Match match = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 5);
        List<User> players = List.of(alice, bob, charlie, dave);

        List<PlayerRanking> rankings = strategy.calculateRankings(players, List.of(match));

        // Winners should be ranked higher than losers
        PlayerRanking aliceRank = rankings.stream().filter(r -> r.userId().equals(alice.getId())).findFirst().orElseThrow();
        PlayerRanking charlieRank = rankings.stream().filter(r -> r.userId().equals(charlie.getId())).findFirst().orElseThrow();

        assertTrue(aliceRank.score() > 1000, "Winner ELO should increase from 1000, got " + aliceRank.score());
        assertTrue(charlieRank.score() < 1000, "Loser ELO should decrease from 1000, got " + charlieRank.score());
    }

    @Test
    void equalTeams_winnersGainMoreThanLosersLose() {
        // Win bonus is one-sided: winners get +0.2, losers get 0 (not -0.2)
        // So total ELO in the system increases after each match (not zero-sum)
        Match match = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 5);
        List<User> players = List.of(alice, bob, charlie, dave);

        List<PlayerRanking> rankings = strategy.calculateRankings(players, List.of(match));

        double totalChange = rankings.stream().mapToDouble(r -> r.score() - 1000).sum();
        assertTrue(totalChange > 0, "Total ELO should increase (win bonus is one-sided). Got " + totalChange);
    }

    @Test
    void equalTeams_drawKeepsRatingsClose() {
        // Draw: 5-5
        Match match = TestDataFactory.createMatch(alice, bob, charlie, dave, 5, 5);
        List<User> players = List.of(alice, bob, charlie, dave);

        List<PlayerRanking> rankings = strategy.calculateRankings(players, List.of(match));

        // All players started equal → draw → all should remain at ~1000
        for (PlayerRanking pr : rankings) {
            assertEquals(1000, pr.score(), 1.0,
                pr.displayName() + " should stay ~1000 after a draw between equal teams");
        }
    }

    @Test
    void strongWin_biggerEloChange() {
        // 10-0 win vs 10-8 win: bigger goal diff → bigger ELO change
        Match bigWin = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 0);
        Match smallWin = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 8);

        List<User> players = List.of(alice, bob, charlie, dave);

        List<PlayerRanking> bigWinRankings = strategy.calculateRankings(players, List.of(bigWin));
        List<PlayerRanking> smallWinRankings = strategy.calculateRankings(players, List.of(smallWin));

        double aliceBigWin = bigWinRankings.stream().filter(r -> r.userId().equals(alice.getId())).findFirst().orElseThrow().score();
        double aliceSmallWin = smallWinRankings.stream().filter(r -> r.userId().equals(alice.getId())).findFirst().orElseThrow().score();

        assertTrue(aliceBigWin > aliceSmallWin,
            "10-0 win should yield more ELO than 10-8 win. Big=" + aliceBigWin + " Small=" + aliceSmallWin);
    }

    @Test
    void weakerTeamWins_biggerReward() {
        // Simulate: Alice+Bob at 1400, Charlie+Dave at 1000
        alice.setEloRating(1400);
        bob.setEloRating(1400);
        charlie.setEloRating(1000);
        dave.setEloRating(1000);

        // Underdogs (Charlie+Dave) win 10-5
        Match upset = TestDataFactory.createMatch(charlie, dave, alice, bob, 10, 5);
        // Favorites (Alice+Bob) win 10-5
        Match expected = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 5);

        List<User> players = List.of(alice, bob, charlie, dave);

        // Underdog win
        List<PlayerRanking> upsetRankings = strategy.calculateRankings(players, List.of(upset));
        double charlieUpset = upsetRankings.stream().filter(r -> r.userId().equals(charlie.getId())).findFirst().orElseThrow().score();
        double charlieGain = charlieUpset - 1000;

        // Favorite win
        List<PlayerRanking> expectedRankings = strategy.calculateRankings(players, List.of(expected));
        double aliceExpected = expectedRankings.stream().filter(r -> r.userId().equals(alice.getId())).findFirst().orElseThrow().score();
        double aliceGain = aliceExpected - 1400;

        assertTrue(charlieGain > aliceGain,
            "Underdog winning should gain more ELO. Underdog gain=" + charlieGain + " Favorite gain=" + aliceGain);
    }

    @Test
    void multipleMatches_eloAccumulates() {
        List<User> players = List.of(alice, bob, charlie, dave);
        Match m1 = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 5);
        Match m2 = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 3);
        Match m3 = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 7);

        List<PlayerRanking> rankings = strategy.calculateRankings(players, List.of(m1, m2, m3));

        PlayerRanking aliceR = rankings.stream().filter(r -> r.userId().equals(alice.getId())).findFirst().orElseThrow();
        PlayerRanking charlieR = rankings.stream().filter(r -> r.userId().equals(charlie.getId())).findFirst().orElseThrow();

        // Alice wins all 3 → should be above 1000
        assertTrue(aliceR.score() > 1020, "3 wins should accumulate ELO. Got " + aliceR.score());
        assertTrue(charlieR.score() < 980, "3 losses should drop ELO. Got " + charlieR.score());
    }

    @Test
    void updateRatingsAfterMatch_persistsNewElo() {
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Match match = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 5);

        strategy.updateRatingsAfterMatch(match);

        assertTrue(alice.getEloRating() > 1000, "Alice's ELO should increase after winning");
        assertTrue(charlie.getEloRating() < 1000, "Charlie's ELO should decrease after losing");
        assertEquals(alice.getEloRating(), bob.getEloRating(), "Same-team players get same ELO change");
    }

    @Test
    void eloFormula_knownValues() {
        // Verify with manual calculation:
        // Equal teams (1000 each), Yellow wins 10-5
        // P = 1 / (1 + 10^((1000-1000)/400)) = 0.5
        // S_winner = 0.5 + 0.2 + (10-5)/20 = 0.95
        // MatchPoints = 32 * (0.95 - 0.5) = 14.4 → round to 14
        // New ELO = 1000 + 14 = 1014
        Match match = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 5);
        List<PlayerRanking> rankings = strategy.calculateRankings(List.of(alice, bob, charlie, dave), List.of(match));

        double aliceElo = rankings.stream().filter(r -> r.userId().equals(alice.getId())).findFirst().orElseThrow().score();
        assertEquals(1014, aliceElo, 1.0, "Expected ~1014 for winner with 10-5 between equal teams");

        // S_loser = 0.5 + 0 + (5-10)/20 = 0.5 - 0.25 = 0.25 (NO win bonus for loser)
        // MatchPoints = 32 * (0.25 - 0.5) = 32 * (-0.25) = -8
        // New ELO = 1000 - 8 = 992
        double charlieElo = rankings.stream().filter(r -> r.userId().equals(charlie.getId())).findFirst().orElseThrow().score();
        assertEquals(992, charlieElo, 1.0, "Expected ~992 for loser with 10-5 between equal teams");
    }

    @Test
    void algorithm_returnsELO() {
        assertEquals(LongTermAlgorithm.ELO, strategy.algorithm());
    }

    @Test
    void noMatches_allPlayersAtDefault() {
        List<PlayerRanking> rankings = strategy.calculateRankings(List.of(alice, bob), List.of());

        for (PlayerRanking pr : rankings) {
            assertEquals(1000, pr.score(), "Players with no matches should be at default 1000");
        }
    }

    @Test
    void rankings_areSortedDescending() {
        // Alice+Bob always win → should be ranked 1st and 2nd
        Match m1 = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 2);
        List<PlayerRanking> rankings = strategy.calculateRankings(
            List.of(alice, bob, charlie, dave), List.of(m1));

        assertEquals(1, rankings.get(0).rank());
        assertEquals(2, rankings.get(1).rank());
        assertEquals(3, rankings.get(2).rank());
        assertEquals(4, rankings.get(3).rank());
        assertTrue(rankings.get(0).score() >= rankings.get(1).score());
        assertTrue(rankings.get(2).score() >= rankings.get(3).score());
    }
}
