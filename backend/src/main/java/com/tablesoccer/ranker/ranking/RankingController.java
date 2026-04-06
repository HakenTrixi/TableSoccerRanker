package com.tablesoccer.ranker.ranking;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rankings")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/long-term")
    public List<PlayerRanking> longTermRankings() {
        return rankingService.getLongTermRankings();
    }

    @GetMapping("/monthly")
    public List<PlayerRanking> monthlyRankings(
            @RequestParam(required = false) String month) {
        YearMonth ym = month != null ? YearMonth.parse(month) : YearMonth.now();
        return rankingService.getMonthlyRankings(ym);
    }

    @GetMapping("/history/{userId}")
    public List<EloSnapshotDto> eloHistory(@PathVariable UUID userId) {
        return rankingService.getEloHistory(userId).stream()
            .map(s -> new EloSnapshotDto(s.getSnapshotDate(), s.getEloRating()))
            .toList();
    }

    @GetMapping("/elo-timeline")
    public List<RankingService.PlayerEloTimeline> eloTimeline(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant fromInstant = from != null
            ? LocalDate.parse(from).atStartOfDay(ZoneOffset.UTC).toInstant()
            : Instant.EPOCH;
        Instant toInstant = to != null
            ? LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
            : Instant.now();
        return rankingService.getEloTimeline(fromInstant, toInstant);
    }

    record EloSnapshotDto(java.time.LocalDate date, int eloRating) {}
}
