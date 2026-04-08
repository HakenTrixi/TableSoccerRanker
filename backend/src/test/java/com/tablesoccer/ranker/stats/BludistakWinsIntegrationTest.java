package com.tablesoccer.ranker.stats;

import com.tablesoccer.ranker.match.*;
import com.tablesoccer.ranker.user.Role;
import com.tablesoccer.ranker.user.User;
import com.tablesoccer.ranker.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying that bludistak (monthly title) wins are computed
 * using ALL matches, not just the target player's matches.
 *
 * Bug: computeBludistakWinners was called with only the player's matches,
 * causing that player to "win" months where another player actually had
 * a higher total ELO gain (from matches the target player didn't participate in).
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class BludistakWinsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired UserRepository userRepository;
    @Autowired MatchRepository matchRepository;
    @Autowired StatsService statsService;

    User playerA, playerB, playerC, playerD, playerE;

    @BeforeEach
    void setUp() {
        playerA = saveUser("PlayerA");
        playerB = saveUser("PlayerB");
        playerC = saveUser("PlayerC");
        playerD = saveUser("PlayerD");
        playerE = saveUser("PlayerE");
    }

    /**
     * Month 1 (Jan 2025):
     *   Match 1 (A plays): A(+10), B(+5), C(-5), D(-10)
     *   Match 2 (A absent): B(+20), C(+20), D(-20), E(-20)
     *   Totals: A=+10, B=+25, C=+15, D=-30, E=-20
     *   → True winner: B (+25)
     *
     * Month 2 (Feb 2025):
     *   Match 3 (A plays): A(+30), B(+15), C(-15), D(-30)
     *   Match 4 (A absent): B(+5), C(+5), D(-5), E(-5)
     *   Totals: A=+30, B=+20, C=-10, D=-35, E=-5
     *   → True winner: A (+30)
     *
     * Expected: A has 1 bludistak win (month 2 only).
     * Bug would give: A = 2 wins (because match 2 & 4 excluded from calculation).
     */
    @Test
    void bludistakWins_shouldUseAllMatchesNotJustPlayerMatches() {
        Instant jan = YearMonth.of(2025, 1).atDay(15).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant feb = YearMonth.of(2025, 2).atDay(15).atStartOfDay().toInstant(ZoneOffset.UTC);

        // Month 1: Match where A participates — A gains most among these 4 players
        createMatchWithElo(jan, playerA, playerB, playerC, playerD,
                10, 5,
                +10, +5, -5, -10);

        // Month 1: Match where A does NOT participate — B gains big
        createMatchWithElo(jan.plusSeconds(3600), playerB, playerC, playerD, playerE,
                10, 2,
                +20, +20, -20, -20);

        // Month 2: Match where A participates — A gains the most overall
        createMatchWithElo(feb, playerA, playerB, playerC, playerD,
                10, 0,
                +30, +15, -15, -30);

        // Month 2: Match where A does NOT participate — small gains
        createMatchWithElo(feb.plusSeconds(3600), playerB, playerC, playerD, playerE,
                6, 5,
                +5, +5, -5, -5);

        PlayerStats stats = statsService.getPlayerStats(playerA.getId());

        assertEquals(1, stats.bludistakWins(),
                "Player A should win only month 2; in month 1 player B has higher total ELO gain (+25 vs +10)");
    }

    @Test
    void bludistakWins_playerWithNoWins_shouldBeZero() {
        Instant jan = YearMonth.of(2025, 1).atDay(15).atStartOfDay().toInstant(ZoneOffset.UTC);

        // Single match: B gains more than A
        createMatchWithElo(jan, playerA, playerB, playerC, playerD,
                10, 5,
                +5, +15, -5, -10);

        PlayerStats stats = statsService.getPlayerStats(playerA.getId());

        assertEquals(0, stats.bludistakWins(),
                "Player A should have 0 wins when B had higher ELO gain");
    }

    private User saveUser(String name) {
        var user = new User();
        user.setDisplayName(name);
        user.setEmail(name.toLowerCase() + UUID.randomUUID() + "@test.com");
        user.setUsername(name.toLowerCase() + UUID.randomUUID());
        user.setRole(Role.PLAYER);
        user.setEloRating(1000);
        user.setAttackerElo(1000);
        user.setDefenderElo(1000);
        return userRepository.save(user);
    }

    private void createMatchWithElo(Instant playedAt,
                                     User yellowAtt, User yellowDef,
                                     User whiteAtt, User whiteDef,
                                     int yellowScore, int whiteScore,
                                     int yAttEloChange, int yDefEloChange,
                                     int wAttEloChange, int wDefEloChange) {
        var match = new Match();
        match.setYellowScore(yellowScore);
        match.setWhiteScore(whiteScore);
        match.setPlayedAt(playedAt);
        match.setRecordedBy(yellowAtt);
        match.addPlayer(yellowAtt, TeamColor.YELLOW, PlayerRole.ATTACKER);
        match.addPlayer(yellowDef, TeamColor.YELLOW, PlayerRole.DEFENDER);
        match.addPlayer(whiteAtt, TeamColor.WHITE, PlayerRole.ATTACKER);
        match.addPlayer(whiteDef, TeamColor.WHITE, PlayerRole.DEFENDER);

        for (MatchPlayer mp : match.getPlayers()) {
            UUID uid = mp.getUser().getId();
            if (uid.equals(yellowAtt.getId())) mp.setEloChange(yAttEloChange);
            else if (uid.equals(yellowDef.getId())) mp.setEloChange(yDefEloChange);
            else if (uid.equals(whiteAtt.getId())) mp.setEloChange(wAttEloChange);
            else if (uid.equals(whiteDef.getId())) mp.setEloChange(wDefEloChange);
        }

        matchRepository.save(match);
    }
}
