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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that profile-page attacker/defender ELO is computed as
 * 1000 + sum(eloChange) grouped by playerRole for the given user.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class StatsServicePositionEloTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired UserRepository userRepository;
    @Autowired MatchRepository matchRepository;
    @Autowired StatsService statsService;

    User target, p2, p3, p4;

    @BeforeEach
    void setUp() {
        target = saveUser("Target");
        p2 = saveUser("Two");
        p3 = saveUser("Three");
        p4 = saveUser("Four");
    }

    @Test
    void noMatches_bothEloAreDefault() {
        PlayerStats stats = statsService.getPlayerStats(target.getId());

        assertEquals(1000, stats.attackerElo());
        assertEquals(1000, stats.defenderElo());
    }

    @Test
    void onlyAttackerMatches_sumsAttackerChanges() {
        createMatch(Instant.parse("2025-01-01T10:00:00Z"),
            target, p2, p3, p4,
            PlayerRole.ATTACKER, 15);
        createMatch(Instant.parse("2025-01-02T10:00:00Z"),
            target, p2, p3, p4,
            PlayerRole.ATTACKER, -7);
        createMatch(Instant.parse("2025-01-03T10:00:00Z"),
            target, p2, p3, p4,
            PlayerRole.ATTACKER, 10);

        PlayerStats stats = statsService.getPlayerStats(target.getId());

        assertEquals(1018, stats.attackerElo()); // 1000 + 15 - 7 + 10
        assertEquals(1000, stats.defenderElo());
    }

    @Test
    void onlyDefenderMatches_sumsDefenderChanges() {
        createMatch(Instant.parse("2025-01-01T10:00:00Z"),
            target, p2, p3, p4,
            PlayerRole.DEFENDER, 20);
        createMatch(Instant.parse("2025-01-02T10:00:00Z"),
            target, p2, p3, p4,
            PlayerRole.DEFENDER, -5);

        PlayerStats stats = statsService.getPlayerStats(target.getId());

        assertEquals(1000, stats.attackerElo());
        assertEquals(1015, stats.defenderElo()); // 1000 + 20 - 5
    }

    @Test
    void mixedRoles_computesBothIndependently() {
        createMatch(Instant.parse("2025-01-01T10:00:00Z"),
            target, p2, p3, p4,
            PlayerRole.ATTACKER, 12);
        createMatch(Instant.parse("2025-01-02T10:00:00Z"),
            target, p2, p3, p4,
            PlayerRole.DEFENDER, 8);
        createMatch(Instant.parse("2025-01-03T10:00:00Z"),
            target, p2, p3, p4,
            PlayerRole.ATTACKER, -4);
        createMatch(Instant.parse("2025-01-04T10:00:00Z"),
            target, p2, p3, p4,
            PlayerRole.DEFENDER, -3);

        PlayerStats stats = statsService.getPlayerStats(target.getId());

        assertEquals(1008, stats.attackerElo()); // 1000 + 12 - 4
        assertEquals(1005, stats.defenderElo()); // 1000 + 8 - 3
    }

    @Test
    void nullEloChange_isIgnored() {
        // Legacy match without eloChange populated
        createMatch(Instant.parse("2025-01-01T10:00:00Z"),
            target, p2, p3, p4,
            PlayerRole.ATTACKER, null);
        createMatch(Instant.parse("2025-01-02T10:00:00Z"),
            target, p2, p3, p4,
            PlayerRole.ATTACKER, 25);

        PlayerStats stats = statsService.getPlayerStats(target.getId());

        assertEquals(1025, stats.attackerElo()); // null row ignored
        assertEquals(1000, stats.defenderElo());
    }

    private User saveUser(String name) {
        var user = new User();
        user.setDisplayName(name);
        user.setEmail(name.toLowerCase() + UUID.randomUUID() + "@test.com");
        user.setUsername(name.toLowerCase() + UUID.randomUUID());
        user.setRole(Role.PLAYER);
        user.setEloRating(1000);
        return userRepository.save(user);
    }

    /**
     * Creates a match where `target` plays in the given role on the yellow team,
     * with the given eloChange. Other players are filled in around target so that
     * the 4-player slot constraint is satisfied.
     */
    private void createMatch(Instant playedAt, User target, User p2, User p3, User p4,
                             PlayerRole targetRole, Integer targetEloChange) {
        var match = new Match();
        match.setYellowScore(10);
        match.setWhiteScore(5);
        match.setPlayedAt(playedAt);
        match.setRecordedBy(target);

        if (targetRole == PlayerRole.ATTACKER) {
            match.addPlayer(target, TeamColor.YELLOW, PlayerRole.ATTACKER);
            match.addPlayer(p2, TeamColor.YELLOW, PlayerRole.DEFENDER);
        } else {
            match.addPlayer(p2, TeamColor.YELLOW, PlayerRole.ATTACKER);
            match.addPlayer(target, TeamColor.YELLOW, PlayerRole.DEFENDER);
        }
        match.addPlayer(p3, TeamColor.WHITE, PlayerRole.ATTACKER);
        match.addPlayer(p4, TeamColor.WHITE, PlayerRole.DEFENDER);

        for (MatchPlayer mp : match.getPlayers()) {
            if (mp.getUser().getId().equals(target.getId())) {
                mp.setEloChange(targetEloChange);
            } else {
                mp.setEloChange(0);
            }
        }

        matchRepository.save(match);
    }
}
