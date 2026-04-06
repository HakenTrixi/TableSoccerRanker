package com.tablesoccer.ranker.team;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
public class TeamFormationController {

    private final TeamFormationService teamFormationService;

    public TeamFormationController(TeamFormationService teamFormationService) {
        this.teamFormationService = teamFormationService;
    }

    @PostMapping("/suggest")
    public TeamSuggestion suggestTeams(@RequestBody TeamSuggestRequest request) {
        return teamFormationService.suggestTeams(request.playerIds());
    }

    record TeamSuggestRequest(List<UUID> playerIds) {}
}
