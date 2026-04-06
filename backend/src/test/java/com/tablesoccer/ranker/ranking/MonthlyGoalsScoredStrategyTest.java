package com.tablesoccer.ranker.ranking;

import com.tablesoccer.ranker.TestDataFactory;
import com.tablesoccer.ranker.match.Match;
import com.tablesoccer.ranker.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MonthlyGoalsScoredStrategyTest {

    MonthlyGoalsScoredStrategy strategy;
    User alice, bob, charlie, dave;

    @BeforeEach
    void setUp() {
        strategy = new MonthlyGoalsScoredStrategy();
        alice = TestDataFactory.createUser("Alice");
        bob = TestDataFactory.createUser("Bob");
        charlie = TestDataFactory.createUser("Charlie");
        dave = TestDataFactory.createUser("Dave");
    }

    @Test
    void singleMatch_goalsAssignedToTeam() {
        // Yellow scores 10, White scores 3
        Match match = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 3);

        List<PlayerRanking> rankings = strategy.calculateRankings(
            List.of(alice, bob, charlie, dave), List.of(match), YearMonth.now());

        PlayerRanking aliceR = rankings.stream().filter(r -> r.userId().equals(alice.getId())).findFirst().orElseThrow();
        PlayerRanking charlieR = rankings.stream().filter(r -> r.userId().equals(charlie.getId())).findFirst().orElseThrow();

        assertEquals(10, aliceR.score(), "Yellow team player should have 10 goals");
        assertEquals(3, charlieR.score(), "White team player should have 3 goals");
    }

    @Test
    void multipleMatches_goalsAccumulate() {
        Match m1 = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 5);
        Match m2 = TestDataFactory.createMatch(alice, bob, charlie, dave, 8, 7);

        List<PlayerRanking> rankings = strategy.calculateRankings(
            List.of(alice, bob, charlie, dave), List.of(m1, m2), YearMonth.now());

        PlayerRanking aliceR = rankings.stream().filter(r -> r.userId().equals(alice.getId())).findFirst().orElseThrow();
        assertEquals(18, aliceR.score(), "10 + 8 = 18 total goals");
    }

    @Test
    void playersWithNoMatches_excluded() {
        var eve = TestDataFactory.createUser("Eve");
        Match match = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 5);

        List<PlayerRanking> rankings = strategy.calculateRankings(
            List.of(alice, bob, charlie, dave, eve), List.of(match), YearMonth.now());

        assertTrue(rankings.stream().noneMatch(r -> r.userId().equals(eve.getId())),
            "Player with no matches should not appear in rankings");
    }

    @Test
    void rankings_sortedDescending() {
        Match match = TestDataFactory.createMatch(alice, bob, charlie, dave, 10, 3);
        List<PlayerRanking> rankings = strategy.calculateRankings(
            List.of(alice, bob, charlie, dave), List.of(match), YearMonth.now());

        assertEquals(10, rankings.get(0).score());
        assertEquals(1, rankings.get(0).rank());
    }

    @Test
    void algorithm_returnsMONTHLY_GOALS_SCORED() {
        assertEquals(MonthlyAlgorithm.MONTHLY_GOALS_SCORED, strategy.algorithm());
    }
}
