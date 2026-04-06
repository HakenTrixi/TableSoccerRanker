package com.tablesoccer.ranker.stats;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/company")
    public CompanyStats companyStats() {
        return statsService.getCompanyStats();
    }

    @GetMapping("/player/{userId}")
    public PlayerStats playerStats(@PathVariable UUID userId) {
        return statsService.getPlayerStats(userId);
    }
}
