package com.tablesoccer.ranker.match;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchDto(
    UUID id,
    int yellowScore,
    int whiteScore,
    Instant playedAt,
    List<MatchPlayerDto> players
) {
    public static MatchDto from(Match match) {
        return new MatchDto(
            match.getId(),
            match.getYellowScore(),
            match.getWhiteScore(),
            match.getPlayedAt(),
            match.getPlayers().stream().map(MatchPlayerDto::from).toList()
        );
    }
}
