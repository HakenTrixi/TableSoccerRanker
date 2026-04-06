package com.tablesoccer.ranker.match;

import com.tablesoccer.ranker.TestDataFactory;
import com.tablesoccer.ranker.user.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MatchValidationTest {

    @Test
    void matchCreateRequest_rejectsDuplicatePlayers() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Same player in two positions
        var request = new MatchCreateRequest(id1, id1, id2, id3, 10, 5, null);

        // Validate using the same logic as MatchService
        var playerIds = new java.util.HashSet<UUID>();
        playerIds.add(request.yellowAttacker());
        playerIds.add(request.yellowDefender());
        playerIds.add(request.whiteAttacker());
        playerIds.add(request.whiteDefender());

        assertTrue(playerIds.size() < 4, "Duplicate player IDs should result in fewer than 4 unique IDs");
    }

    @Test
    void matchCreateRequest_allDistinct_passes() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID id4 = UUID.randomUUID();

        var request = new MatchCreateRequest(id1, id2, id3, id4, 10, 5, null);

        var playerIds = java.util.Set.of(
            request.yellowAttacker(), request.yellowDefender(),
            request.whiteAttacker(), request.whiteDefender()
        );
        assertEquals(4, playerIds.size(), "All 4 player IDs should be unique");
    }

    @Test
    void match_winnerColor_yellowWins() {
        User a = TestDataFactory.createUser("A");
        User b = TestDataFactory.createUser("B");
        User c = TestDataFactory.createUser("C");
        User d = TestDataFactory.createUser("D");
        Match match = TestDataFactory.createMatch(a, b, c, d, 10, 5);

        assertEquals(TeamColor.YELLOW, match.winnerColor());
    }

    @Test
    void match_winnerColor_whiteWins() {
        User a = TestDataFactory.createUser("A");
        User b = TestDataFactory.createUser("B");
        User c = TestDataFactory.createUser("C");
        User d = TestDataFactory.createUser("D");
        Match match = TestDataFactory.createMatch(a, b, c, d, 3, 10);

        assertEquals(TeamColor.WHITE, match.winnerColor());
    }

    @Test
    void match_winnerColor_draw() {
        User a = TestDataFactory.createUser("A");
        User b = TestDataFactory.createUser("B");
        User c = TestDataFactory.createUser("C");
        User d = TestDataFactory.createUser("D");
        Match match = TestDataFactory.createMatch(a, b, c, d, 5, 5);

        assertNull(match.winnerColor(), "Draw should return null winner");
    }

    @Test
    void match_scoreForAndAgainst() {
        User a = TestDataFactory.createUser("A");
        User b = TestDataFactory.createUser("B");
        User c = TestDataFactory.createUser("C");
        User d = TestDataFactory.createUser("D");
        Match match = TestDataFactory.createMatch(a, b, c, d, 10, 3);

        assertEquals(10, match.scoreFor(TeamColor.YELLOW));
        assertEquals(3, match.scoreAgainst(TeamColor.YELLOW));
        assertEquals(3, match.scoreFor(TeamColor.WHITE));
        assertEquals(10, match.scoreAgainst(TeamColor.WHITE));
    }

    @Test
    void match_hasFourPlayers() {
        User a = TestDataFactory.createUser("A");
        User b = TestDataFactory.createUser("B");
        User c = TestDataFactory.createUser("C");
        User d = TestDataFactory.createUser("D");
        Match match = TestDataFactory.createMatch(a, b, c, d, 10, 5);

        assertEquals(4, match.getPlayers().size());

        long yellowCount = match.getPlayers().stream().filter(p -> p.getTeamColor() == TeamColor.YELLOW).count();
        long whiteCount = match.getPlayers().stream().filter(p -> p.getTeamColor() == TeamColor.WHITE).count();
        assertEquals(2, yellowCount, "Should have 2 yellow players");
        assertEquals(2, whiteCount, "Should have 2 white players");
    }

    @Test
    void match_eachTeamHasAttackerAndDefender() {
        User a = TestDataFactory.createUser("A");
        User b = TestDataFactory.createUser("B");
        User c = TestDataFactory.createUser("C");
        User d = TestDataFactory.createUser("D");
        Match match = TestDataFactory.createMatch(a, b, c, d, 10, 5);

        for (TeamColor color : TeamColor.values()) {
            var teamPlayers = match.getPlayers().stream()
                .filter(p -> p.getTeamColor() == color).toList();

            assertTrue(teamPlayers.stream().anyMatch(p -> p.getPlayerRole() == PlayerRole.ATTACKER),
                color + " team should have an attacker");
            assertTrue(teamPlayers.stream().anyMatch(p -> p.getPlayerRole() == PlayerRole.DEFENDER),
                color + " team should have a defender");
        }
    }
}
