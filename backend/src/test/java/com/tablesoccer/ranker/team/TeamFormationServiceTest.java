package com.tablesoccer.ranker.team;

import com.tablesoccer.ranker.ranking.PlayerRanking;
import com.tablesoccer.ranker.ranking.RankingService;
import com.tablesoccer.ranker.user.Role;
import com.tablesoccer.ranker.user.UserDto;
import com.tablesoccer.ranker.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamFormationServiceTest {

    @Mock
    RankingService rankingService;
    @Mock
    UserService userService;

    TeamFormationService service;

    UUID bestId = UUID.randomUUID();
    UUID secondId = UUID.randomUUID();
    UUID thirdId = UUID.randomUUID();
    UUID worstId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TeamFormationService(rankingService, userService);

        // Rankings: best=1500, 2nd=1300, 3rd=1100, worst=900
        when(rankingService.getLongTermRankings()).thenReturn(List.of(
            new PlayerRanking(1, bestId, "Best", null, 1500),
            new PlayerRanking(2, secondId, "Second", null, 1300),
            new PlayerRanking(3, thirdId, "Third", null, 1100),
            new PlayerRanking(4, worstId, "Worst", null, 900)
        ));

        when(userService.findById(bestId)).thenReturn(new UserDto(bestId, "Best", "b@t.com", null, Role.PLAYER, 1500, true));
        when(userService.findById(secondId)).thenReturn(new UserDto(secondId, "Second", "s@t.com", null, Role.PLAYER, 1300, true));
        when(userService.findById(thirdId)).thenReturn(new UserDto(thirdId, "Third", "t@t.com", null, Role.PLAYER, 1100, true));
        when(userService.findById(worstId)).thenReturn(new UserDto(worstId, "Worst", "w@t.com", null, Role.PLAYER, 900, true));
    }

    @Test
    void suggestTeams_bestPlusWorstVsSecondPlusThird() {
        TeamSuggestion suggestion = service.suggestTeams(List.of(bestId, secondId, thirdId, worstId));

        // Yellow = best (attacker) + worst (defender)
        assertEquals(bestId, suggestion.yellowAttacker().id(), "Yellow attacker should be best player");
        assertEquals(worstId, suggestion.yellowDefender().id(), "Yellow defender should be worst player");

        // White = 2nd (attacker) + 3rd (defender)
        assertEquals(secondId, suggestion.whiteAttacker().id(), "White attacker should be 2nd player");
        assertEquals(thirdId, suggestion.whiteDefender().id(), "White defender should be 3rd player");
    }

    @Test
    void suggestTeams_teamsAreBalanced() {
        TeamSuggestion suggestion = service.suggestTeams(List.of(bestId, secondId, thirdId, worstId));

        int yellowTotalElo = suggestion.yellowAttacker().eloRating() + suggestion.yellowDefender().eloRating();
        int whiteTotalElo = suggestion.whiteAttacker().eloRating() + suggestion.whiteDefender().eloRating();

        // Best(1500) + Worst(900) = 2400 vs Second(1300) + Third(1100) = 2400
        assertEquals(yellowTotalElo, whiteTotalElo, "Teams should be balanced: " + yellowTotalElo + " vs " + whiteTotalElo);
    }

    @Test
    void suggestTeams_orderOfInputDoesNotMatter() {
        // Provide IDs in random order
        TeamSuggestion s1 = service.suggestTeams(List.of(worstId, bestId, thirdId, secondId));
        TeamSuggestion s2 = service.suggestTeams(List.of(thirdId, secondId, worstId, bestId));

        assertEquals(s1.yellowAttacker().id(), s2.yellowAttacker().id());
        assertEquals(s1.yellowDefender().id(), s2.yellowDefender().id());
        assertEquals(s1.whiteAttacker().id(), s2.whiteAttacker().id());
        assertEquals(s1.whiteDefender().id(), s2.whiteDefender().id());
    }

    @Test
    void suggestTeams_rejectsLessThan4Players() {
        assertThrows(IllegalArgumentException.class,
            () -> service.suggestTeams(List.of(bestId, secondId, thirdId)));
    }

    @Test
    void suggestTeams_rejectsMoreThan4Players() {
        UUID fifthId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
            () -> service.suggestTeams(List.of(bestId, secondId, thirdId, worstId, fifthId)));
    }

    @Test
    void suggestTeams_rejectsDuplicatePlayers() {
        assertThrows(IllegalArgumentException.class,
            () -> service.suggestTeams(List.of(bestId, bestId, thirdId, worstId)));
    }
}
