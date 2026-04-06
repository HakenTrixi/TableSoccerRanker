package com.tablesoccer.ranker.ranking;

import com.tablesoccer.ranker.TestDataFactory;
import com.tablesoccer.ranker.match.Match;
import com.tablesoccer.ranker.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AvgGoalDiffRankingStrategyTest {

    AvgGoalDiffRankingStrategy strategy;
    User alice, bob, charlie, dave;

    @BeforeEach
    void setUp() {
        strategy = new AvgGoalDiffRankingStrategy();
        alice = TestDataFactory.createUser("Alice");
        bob = TestDataFactory.createUser("Bob");
        charlie = TestDataFactory.createUser("Charlie");
        dave = TestDataFactory.createUser("Dave");
    }

    @Test
    void singleMatch_winnerHasPositiveAvg() {
        // Yellow wins 10-3 → yellow players: avg = (10-3) = +7, white: avg = (3-10) = -7
        Match match = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 3);

        List<PlayerRanking> rankings = strategy.calculateRankings(
            List.of(alice, bob, charlie, dave), List.of(match));

        PlayerRanking aliceR = rankings.stream().filter(r -> r.userId().equals(alice.getId())).findFirst().orElseThrow();
        PlayerRanking charlieR = rankings.stream().filter(r -> r.userId().equals(charlie.getId())).findFirst().orElseThrow();

        assertEquals(7.0, aliceR.score(), 0.01, "Winner avg goal diff should be +7");
        assertEquals(-7.0, charlieR.score(), 0.01, "Loser avg goal diff should be -7");
    }

    @Test
    void multipleMatches_averageIsComputed() {
        // Alice plays 2 matches: +5 and -3 → avg = +1
        Match m1 = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 5);  // yellow +5
        Match m2 = TestDataFactory.createMatch(charlie, dave, alice, bob, 10, 7);  // alice is white, 7-10 = -3

        List<PlayerRanking> rankings = strategy.calculateRankings(
            List.of(alice, bob, charlie, dave), List.of(m1, m2));

        PlayerRanking aliceR = rankings.stream().filter(r -> r.userId().equals(alice.getId())).findFirst().orElseThrow();
        assertEquals(1.0, aliceR.score(), 0.01, "Average of +5 and -3 should be +1");
    }

    @Test
    void draw_avgIsZero() {
        Match match = TestDataFactory.createMatch(alice, bob, charlie, dave, 5, 5);

        List<PlayerRanking> rankings = strategy.calculateRankings(
            List.of(alice, bob, charlie, dave), List.of(match));

        for (PlayerRanking r : rankings) {
            assertEquals(0.0, r.score(), 0.01, "Draw should yield 0 avg goal diff");
        }
    }

    @Test
    void noMatches_allZero() {
        List<PlayerRanking> rankings = strategy.calculateRankings(
            List.of(alice, bob), List.of());

        for (PlayerRanking r : rankings) {
            assertEquals(0.0, r.score(), 0.01);
        }
    }

    @Test
    void rankings_sortedByScoreDescending() {
        Match m1 = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 2); // +8 vs -8
        List<PlayerRanking> rankings = strategy.calculateRankings(
            List.of(alice, bob, charlie, dave), List.of(m1));

        assertEquals(1, rankings.get(0).rank());
        assertTrue(rankings.get(0).score() > rankings.get(2).score());
    }

    @Test
    void updateRatingsAfterMatch_isNoOp() {
        Match match = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 5);
        // Should not throw — it's a no-op for this strategy
        assertDoesNotThrow(() -> strategy.updateRatingsAfterMatch(match));
    }

    @Test
    void algorithm_returnsAVG_GOAL_DIFF() {
        assertEquals(LongTermAlgorithm.AVG_GOAL_DIFF, strategy.algorithm());
    }
}
