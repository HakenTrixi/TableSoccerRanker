package com.tablesoccer.ranker.stats;

import com.tablesoccer.ranker.TestDataFactory;
import com.tablesoccer.ranker.match.*;
import com.tablesoccer.ranker.user.User;
import com.tablesoccer.ranker.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests StatsService streak and recent form computation.
 * Based on real production data for Lukáš:
 *   Match 1 (oldest): Lukáš on YELLOW, yellow wins 10:5 → WIN
 *   Match 2: Lukáš on WHITE, white wins 5:10 → WIN
 *   Match 3: Lukáš on YELLOW, yellow loses 3:10 → LOSS
 *   Match 4: Lukáš on WHITE, white wins 8:10 → WIN
 *   Match 5 (newest): Lukáš on WHITE, yellow wins 10:7 → LOSS
 *
 * Expected: currentStreak = 1L, recentForm last = LOSS,
 *           longestWinStreak = 2, longestLoseStreak = 1
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class StatsServiceTest {

    @Mock MatchRepository matchRepository;
    @Mock MatchPlayerRepository matchPlayerRepository;
    @Mock UserRepository userRepository;

    StatsService service;

    User lukas, partner, opp1, opp2;

    @BeforeEach
    void setUp() {
        service = new StatsService(matchRepository, matchPlayerRepository, userRepository);
        lukas = TestDataFactory.createUser("Lukáš", 2033);
        partner = TestDataFactory.createUser("Partner", 1500);
        opp1 = TestDataFactory.createUser("Opp1", 1400);
        opp2 = TestDataFactory.createUser("Opp2", 1400);
    }

    private Match createMatch(User yAtt, User yDef, User wAtt, User wDef, int yScore, int wScore) {
        return TestDataFactory.createMatch(yAtt, yDef, wAtt, wDef, yScore, wScore);
    }

    @Test
    void currentStreak_reflectsLastMatch() {
        // Matches in chronological order (ASC)
        Match m1 = createMatch(lukas, partner, opp1, opp2, 10, 5);   // Lukáš YELLOW ATT, yellow wins → WIN
        Match m2 = createMatch(opp1, opp2, lukas, partner, 5, 10);    // Lukáš WHITE ATT, white wins → WIN
        Match m3 = createMatch(lukas, partner, opp1, opp2, 3, 10);    // Lukáš YELLOW ATT, yellow loses → LOSS
        Match m4 = createMatch(opp1, opp2, lukas, partner, 8, 10);    // Lukáš WHITE ATT, white wins → WIN
        Match m5 = createMatch(opp1, opp2, lukas, partner, 10, 7);    // Lukáš WHITE ATT, yellow wins → LOSS

        // findAllByPlayerId should return in ASC order (oldest first)
        when(matchRepository.findAllByPlayerId(lukas.getId())).thenReturn(List.of(m1, m2, m3, m4, m5));
        when(matchRepository.findAllByOrderByPlayedAtAsc()).thenReturn(List.of(m1, m2, m3, m4, m5));
        when(userRepository.findById(lukas.getId())).thenReturn(Optional.of(lukas));
        when(userRepository.findAll()).thenReturn(List.of(lukas, partner, opp1, opp2));
        when(matchPlayerRepository.findByUserIdWithEloData(any())).thenReturn(List.of());
        when(userRepository.findByActiveTrue()).thenReturn(List.of(lukas, partner, opp1, opp2));

        PlayerStats stats = service.getPlayerStats(lukas.getId());

        // Last match (m5) is a LOSS → current streak should be 1L
        assertEquals("LOSE", stats.currentStreak().type(), "Last match was a loss");
        assertEquals(1, stats.currentStreak().count(), "Only 1 loss at end");

        // Recent form: last entry should be LOSS (m5)
        var form = stats.recentForm();
        assertFalse(form.isEmpty());
        assertFalse(form.get(form.size() - 1).won(), "Last form entry should be a loss (m5)");

        // First form entry should be WIN (m1)
        assertTrue(form.get(0).won(), "First form entry should be a win (m1)");
    }

    @Test
    void longestStreaks_computedCorrectly() {
        // W W L W L → longestWin=2, longestLose=1
        Match m1 = createMatch(lukas, partner, opp1, opp2, 10, 5);   // WIN
        Match m2 = createMatch(opp1, opp2, lukas, partner, 5, 10);    // WIN
        Match m3 = createMatch(lukas, partner, opp1, opp2, 3, 10);    // LOSS
        Match m4 = createMatch(opp1, opp2, lukas, partner, 8, 10);    // WIN
        Match m5 = createMatch(opp1, opp2, lukas, partner, 10, 7);    // LOSS

        when(matchRepository.findAllByPlayerId(lukas.getId())).thenReturn(List.of(m1, m2, m3, m4, m5));
        when(matchRepository.findAllByOrderByPlayedAtAsc()).thenReturn(List.of(m1, m2, m3, m4, m5));
        when(userRepository.findById(lukas.getId())).thenReturn(Optional.of(lukas));
        when(userRepository.findAll()).thenReturn(List.of(lukas, partner, opp1, opp2));
        when(matchPlayerRepository.findByUserIdWithEloData(any())).thenReturn(List.of());
        when(userRepository.findByActiveTrue()).thenReturn(List.of(lukas, partner, opp1, opp2));

        PlayerStats stats = service.getPlayerStats(lukas.getId());

        assertEquals(2, stats.longestWinStreak(), "W W at start is longest win streak");
        assertEquals(1, stats.longestLoseStreak(), "Single losses only");
    }

    @Test
    void recentForm_lastTenInChronologicalOrder() {
        // Create 12 matches: 10 wins then 2 losses
        var matches = new java.util.ArrayList<Match>();
        for (int i = 0; i < 10; i++) {
            matches.add(createMatch(lukas, partner, opp1, opp2, 10, 5)); // WIN
        }
        matches.add(createMatch(lukas, partner, opp1, opp2, 3, 10)); // LOSS
        matches.add(createMatch(lukas, partner, opp1, opp2, 4, 10)); // LOSS

        when(matchRepository.findAllByPlayerId(lukas.getId())).thenReturn(matches);
        when(matchRepository.findAllByOrderByPlayedAtAsc()).thenReturn(matches);
        when(userRepository.findById(lukas.getId())).thenReturn(Optional.of(lukas));
        when(userRepository.findAll()).thenReturn(List.of(lukas, partner, opp1, opp2));
        when(matchPlayerRepository.findByUserIdWithEloData(any())).thenReturn(List.of());
        when(userRepository.findByActiveTrue()).thenReturn(List.of(lukas, partner, opp1, opp2));

        PlayerStats stats = service.getPlayerStats(lukas.getId());

        // Recent form = last 10: 8 wins + 2 losses
        assertEquals(10, stats.recentForm().size());
        // Last 2 should be losses
        assertFalse(stats.recentForm().get(9).won(), "Last form entry is loss");
        assertFalse(stats.recentForm().get(8).won(), "Second-to-last is loss");
        // First 8 should be wins
        assertTrue(stats.recentForm().get(0).won(), "Oldest in form is win");

        // Current streak should be 2L
        assertEquals("LOSE", stats.currentStreak().type());
        assertEquals(2, stats.currentStreak().count());
    }

    @Test
    void winRecord_countsCorrectly() {
        Match m1 = createMatch(lukas, partner, opp1, opp2, 10, 5);   // WIN
        Match m2 = createMatch(opp1, opp2, lukas, partner, 5, 10);    // WIN
        Match m3 = createMatch(lukas, partner, opp1, opp2, 3, 10);    // LOSS
        Match m4 = createMatch(opp1, opp2, lukas, partner, 5, 5);     // DRAW
        Match m5 = createMatch(opp1, opp2, lukas, partner, 10, 7);    // LOSS

        when(matchRepository.findAllByPlayerId(lukas.getId())).thenReturn(List.of(m1, m2, m3, m4, m5));
        when(matchRepository.findAllByOrderByPlayedAtAsc()).thenReturn(List.of(m1, m2, m3, m4, m5));
        when(userRepository.findById(lukas.getId())).thenReturn(Optional.of(lukas));
        when(userRepository.findAll()).thenReturn(List.of(lukas, partner, opp1, opp2));
        when(matchPlayerRepository.findByUserIdWithEloData(any())).thenReturn(List.of());
        when(userRepository.findByActiveTrue()).thenReturn(List.of(lukas, partner, opp1, opp2));

        PlayerStats stats = service.getPlayerStats(lukas.getId());

        assertEquals(5, stats.totalMatches());
        assertEquals(2, stats.wins());
        assertEquals(2, stats.losses());
        assertEquals(1, stats.draws());
    }
}
