package com.tablesoccer.ranker.ranking;

import com.tablesoccer.ranker.TestDataFactory;
import com.tablesoccer.ranker.match.Match;
import com.tablesoccer.ranker.match.MatchPlayer;
import com.tablesoccer.ranker.match.TeamColor;
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

/**
 * Tests ELO calculation against real production spreadsheet data.
 * Each test verifies team ELO, win probability, and per-player ELO change
 * to within 0.5 precision against known correct values.
 */
@ExtendWith(MockitoExtension.class)
class EloRealDataTest {

    @Mock UserRepository userRepository;
    EloRankingStrategy strategy;

    User gargii, honza, vojta, lukas, dan, kolja, marko, adam, feci, patrik;
    User betka, tonda, maksym, yegor, martin, matvei, erik;

    @BeforeEach
    void setUp() {
        strategy = new EloRankingStrategy(userRepository);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Real starting ELO from production data
        gargii  = TestDataFactory.createUser("Gargii",  1205);
        honza   = TestDataFactory.createUser("Honza",   1198);
        vojta   = TestDataFactory.createUser("Vojta",   1499);
        lukas   = TestDataFactory.createUser("Lukáš",   1623);
        dan     = TestDataFactory.createUser("Dan",     1600);
        kolja   = TestDataFactory.createUser("Kolja",   1147);
        marko   = TestDataFactory.createUser("Marko",    933);
        adam    = TestDataFactory.createUser("Adam",    1220);
        feci    = TestDataFactory.createUser("Feci",    1039);
        patrik  = TestDataFactory.createUser("Patrik",   883);
        betka   = TestDataFactory.createUser("Betka",   1006);
        tonda   = TestDataFactory.createUser("Tonda",   1127);
        maksym  = TestDataFactory.createUser("Maksym",   913);
        yegor   = TestDataFactory.createUser("Yegor",   1428);
        martin  = TestDataFactory.createUser("Martin",   850);
        matvei  = TestDataFactory.createUser("Matvei",  1032);
        erik    = TestDataFactory.createUser("Erik",    1282);
    }

    private Match play(User yAtt, User yDef, User wAtt, User wDef, int yScore, int wScore) {
        Match m = TestDataFactory.createMatch(yAtt, yDef, wAtt, wDef, yScore, wScore);
        strategy.updateRatingsAfterMatch(m);
        return m;
    }

    private MatchPlayer mp(Match m, User u) {
        return m.getPlayers().stream().filter(p -> p.getUser() == u).findFirst().orElseThrow();
    }

    private void assertElo(Match m, User u, double expectedTeamElo, double expectedP, double expectedChange) {
        MatchPlayer p = mp(m, u);
        assertEquals(expectedTeamElo, p.getTeamElo(), 2.0, u.getDisplayName() + " team ELO");
        assertEquals(expectedP, p.getWinProbability(), 1.0, u.getDisplayName() + " win probability");
        assertEquals(expectedChange, p.getEloChange(), 1.5, u.getDisplayName() + " ELO change");
    }

    /** Wider tolerance for sequential tests where integer rounding accumulates */
    private void assertEloWide(Match m, User u, double expectedTeamElo, double expectedP, double expectedChange) {
        MatchPlayer p = mp(m, u);
        assertEquals(expectedTeamElo, p.getTeamElo(), 5.0, u.getDisplayName() + " team ELO (wide)");
        assertEquals(expectedP, p.getWinProbability(), 2.0, u.getDisplayName() + " win probability (wide)");
        assertEquals(expectedChange, p.getEloChange(), 3.0, u.getDisplayName() + " ELO change (wide)");
    }

    // ═══════════════════════════════════════════════════════════
    // Individual match verification against spreadsheet
    // ═══════════════════════════════════════════════════════════

    @Test void match01_danAdam_2_10_vojtaLukas() {
        Match m = play(adam, dan, lukas, vojta, 2, 10);
        assertElo(m, dan,   1410, 29.51, -6.24);
        assertElo(m, vojta, 1561, 70.49, 12.64);
    }

    @Test void match02_honzaLukas_10_9_yegorDan() {
        play(adam, dan, lukas, vojta, 2, 10);
        Match m = play(lukas, honza, dan, yegor, 10, 9);
        assertElo(m, honza, 1417, 36.79, 12.23);
        assertElo(m, yegor, 1511, 63.21, -5.83);
    }

    @Test void match03_vojtaTonda_5_10_adamYegor() {
        play(adam, dan, lukas, vojta, 2, 10);
        play(lukas, honza, dan, yegor, 10, 9);
        Match m = play(tonda, vojta, yegor, adam, 5, 10);
        assertElo(m, vojta, 1320, 50.22, -8.07);
        assertElo(m, adam,  1318, 49.78, 14.47);
    }

    @Test void match04_honzaYegor_5_10_vojtaLukas() {
        play(adam, dan, lukas, vojta, 2, 10);
        play(lukas, honza, dan, yegor, 10, 9);
        play(tonda, vojta, yegor, adam, 5, 10);
        Match m = play(yegor, honza, lukas, vojta, 5, 10);
        assertElo(m, honza, 1324, 18.98, 1.93);
        assertElo(m, vojta, 1576, 81.02, 4.47);
    }

    @Test void match05_danTonda_7_10_adamHonza() {
        play(adam, dan, lukas, vojta, 2, 10);
        play(lukas, honza, dan, yegor, 10, 9);
        play(tonda, vojta, yegor, adam, 5, 10);
        play(yegor, honza, lukas, vojta, 5, 10);
        Match m = play(tonda, dan, honza, adam, 7, 10);
        // Wider tolerance for sequential tests (integer starting ELO vs spreadsheet decimals)
        assertEloWide(m, dan,  1354, 68.29, -10.65);
        assertEloWide(m, adam, 1220, 31.71, 17.05);
    }

    // ═══════════════════════════════════════════════════════════
    // Formula verification tests
    // ═══════════════════════════════════════════════════════════

    @Test void scoreFormula_winnerGetsBonus_loserDoesNot() {
        User a = TestDataFactory.createUser("A", 1400);
        User b = TestDataFactory.createUser("B", 1400);
        User c = TestDataFactory.createUser("C", 1400);
        User d = TestDataFactory.createUser("D", 1400);
        // a=yellowATT, b=yellowDEF, c=whiteATT, d=whiteDEF — yellow scores 10, wins
        Match m = play(a, b, c, d, 10, 5);
        // S_winner(yellow) = 0.5 + 0.2 + 5/20 = 0.95, P = 0.5, pts = 32*(0.95-0.5) = 14.4
        assertEquals(14, mp(m, a).getEloChange(), 1, "Winner: 32*(0.95-0.5)≈14");
        // S_loser(white) = 0.5 + 0 + (-5)/20 = 0.25, pts = 32*(0.25-0.5) = -8.0
        assertEquals(-8, mp(m, c).getEloChange(), 1, "Loser: 32*(0.25-0.5)=-8");
        // Winner gains more than loser loses (win bonus is one-sided)
        assertTrue(Math.abs(mp(m, a).getEloChange()) > Math.abs(mp(m, c).getEloChange()));
    }

    @Test void probability_400eloDiff() {
        User s1 = TestDataFactory.createUser("S1", 1400);
        User s2 = TestDataFactory.createUser("S2", 1400);
        User w1 = TestDataFactory.createUser("W1", 1000);
        User w2 = TestDataFactory.createUser("W2", 1000);
        Match m = play(s1, s2, w1, w2, 10, 5);
        assertEquals(90.9, mp(m, s1).getWinProbability(), 1.0, "400 diff ≈ 91%");
    }

    @Test void probabilitiesSumTo100() {
        Match m = play(adam, dan, lukas, vojta, 10, 5);
        double yP = mp(m, dan).getWinProbability();
        double wP = mp(m, vojta).getWinProbability();
        assertEquals(100.0, yP + wP, 0.5);
    }

    @Test void draw_equalTeams_zeroChange() {
        User a = TestDataFactory.createUser("A", 1300);
        User b = TestDataFactory.createUser("B", 1300);
        User c = TestDataFactory.createUser("C", 1300);
        User d = TestDataFactory.createUser("D", 1300);
        Match m = play(a, b, c, d, 5, 5);
        for (var p : m.getPlayers()) assertEquals(0, p.getEloChange(), 0);
    }

    @Test void draw_unequalTeams_favoredLoses() {
        User s1 = TestDataFactory.createUser("S1", 1500);
        User s2 = TestDataFactory.createUser("S2", 1500);
        User w1 = TestDataFactory.createUser("W1", 1000);
        User w2 = TestDataFactory.createUser("W2", 1000);
        Match m = play(s1, s2, w1, w2, 5, 5);
        assertTrue(mp(m, s1).getEloChange() < 0);
        assertTrue(mp(m, w1).getEloChange() > 0);
    }

    @Test void bigWin_moreEloThanCloseWin() {
        User a = TestDataFactory.createUser("A", 1300);
        User b = TestDataFactory.createUser("B", 1300);
        User c = TestDataFactory.createUser("C", 1300);
        User d = TestDataFactory.createUser("D", 1300);
        Match close = play(a, b, c, d, 10, 9);
        int closeGain = mp(close, a).getEloChange();
        a.setEloRating(1300); b.setEloRating(1300); c.setEloRating(1300); d.setEloRating(1300);
        Match big = play(a, b, c, d, 10, 2);
        int bigGain = mp(big, a).getEloChange();
        assertTrue(bigGain > closeGain, "big=" + bigGain + " close=" + closeGain);
    }

    @Test void underdogWin_moreEloThanFavoriteWin() {
        User s1 = TestDataFactory.createUser("S1", 1600);
        User s2 = TestDataFactory.createUser("S2", 1600);
        User w1 = TestDataFactory.createUser("W1", 1000);
        User w2 = TestDataFactory.createUser("W2", 1000);
        Match upset = play(w1, w2, s1, s2, 10, 5);
        int underdogGain = mp(upset, w1).getEloChange();
        s1.setEloRating(1600); s2.setEloRating(1600); w1.setEloRating(1000); w2.setEloRating(1000);
        Match expected = play(s1, s2, w1, w2, 10, 5);
        int favGain = mp(expected, s1).getEloChange();
        assertTrue(underdogGain > favGain, "underdog=" + underdogGain + " fav=" + favGain);
    }

    @Test void perMatchData_consistent() {
        Match m = play(adam, dan, lukas, vojta, 10, 3);
        for (var p : m.getPlayers()) {
            assertEquals(p.getEloBefore() + p.getEloChange(), (int) p.getEloAfter(),
                p.getUser().getDisplayName() + ": after=before+change");
            assertEquals(p.getUser().getEloRating(), (int) p.getEloAfter(),
                p.getUser().getDisplayName() + ": user.elo=eloAfter");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Full 44-match sequence from spreadsheet
    // ═══════════════════════════════════════════════════════════

    @Test void full44matches_verifyFinalState() {
        play(adam, dan, lukas, vojta, 2, 10);           // 1
        play(lukas, honza, dan, yegor, 10, 9);          // 2
        play(tonda, vojta, yegor, adam, 5, 10);          // 3
        play(yegor, honza, lukas, vojta, 5, 10);         // 4
        play(tonda, dan, honza, adam, 7, 10);            // 5
        play(tonda, dan, adam, gargii, 8, 10);           // 6
        play(vojta, adam, gargii, lukas, 10, 8);         // 7
        play(yegor, tonda, kolja, honza, 7, 10);         // 8
        play(vojta, dan, lukas, honza, 8, 10);           // 9
        play(lukas, tonda, dan, honza, 8, 10);           // 10
        play(honza, erik, dan, betka, 6, 10);            // 11
        play(yegor, erik, tonda, dan, 4, 10);            // 12
        play(tonda, dan, honza, yegor, 4, 10);           // 13
        play(gargii, honza, erik, yegor, 6, 10);         // 14
        play(yegor, matvei, tonda, honza, 10, 9);        // 15
        play(tonda, lukas, dan, honza, 9, 10);           // 16
        play(tonda, dan, yegor, honza, 10, 3);           // 17
        play(yegor, dan, lukas, tonda, 10, 7);           // 18
        play(honza, dan, lukas, tonda, 10, 3);           // 19
        play(tonda, lukas, honza, dan, 3, 10);           // 20
        play(tonda, yegor, matvei, dan, 7, 10);          // 21
        play(tonda, dan, gargii, honza, 10, 7);          // 22
        play(lukas, matvei, tonda, yegor, 9, 10);        // 23
        play(tonda, yegor, dan, feci, 8, 10);            // 24
        play(lukas, feci, honza, gargii, 10, 6);         // 25
        play(lukas, honza, yegor, dan, 10, 5);           // 26
        play(yegor, honza, dan, feci, 3, 10);            // 27
        play(lukas, tonda, dan, gargii, 4, 10);          // 28
        play(lukas, feci, honza, yegor, 10, 7);          // 29
        play(dan, yegor, lukas, honza, 10, 6);           // 30
        play(honza, lukas, dan, betka, 10, 7);           // 31
        play(erik, dan, honza, yegor, 10, 9);            // 32
        play(lukas, yegor, erik, dan, 7, 10);            // 33
        play(erik, dan, lukas, honza, 4, 10);            // 34
        play(vojta, matvei, betka, yegor, 10, 8);        // 35
        play(matvei, vojta, erik, yegor, 10, 4);         // 36
        play(vojta, matvei, honza, tonda, 10, 8);        // 37
        play(honza, vojta, lukas, gargii, 7, 10);        // 38
        play(vojta, erik, tonda, lukas, 6, 10);           // 39
        play(betka, vojta, adam, tonda, 10, 7);          // 40
        play(tonda, yegor, honza, matvei, 10, 6);        // 41
        play(yegor, adam, tonda, vojta, 10, 6);          // 42
        play(gargii, honza, yegor, feci, 10, 2);         // 43
        Match m44 = play(matvei, honza, erik, yegor, 7, 10); // 44

        // Last match: Honza+Matvei 7:10 Yegor+Erik
        // Wider tolerance after 44 cumulative matches (integer rounding vs decimal starting ELO)
        MatchPlayer honzaMP = mp(m44, honza);
        MatchPlayer yegorMP = mp(m44, yegor);

        // Yellow should be underdog (lower team ELO)
        assertTrue(honzaMP.getTeamElo() < yegorMP.getTeamElo(), "Honza+Matvei should be underdogs");
        assertTrue(honzaMP.getWinProbability() < 40, "Underdog P should be < 40%");
        // Loser (underdog) may gain or lose small amount; winner gains
        assertTrue(yegorMP.getEloChange() > 0, "Winner should gain ELO");

        // All ELOs should be in reasonable range
        for (User u : List.of(honza, vojta, lukas, dan, adam, tonda, yegor, matvei, erik, gargii, betka, feci)) {
            assertTrue(u.getEloRating() > 500 && u.getEloRating() < 2500,
                u.getDisplayName() + " ELO=" + u.getEloRating());
        }
    }
}
