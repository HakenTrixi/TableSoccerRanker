package com.tablesoccer.ranker.match;

import java.util.UUID;

public record MatchPlayerDto(
    UUID userId,
    String displayName,
    String avatarUrl,
    TeamColor teamColor,
    PlayerRole playerRole,
    Integer eloBefore,
    Integer eloAfter,
    Integer eloChange,
    Double teamElo,
    Double winProbability
) {
    public static MatchPlayerDto from(MatchPlayer mp) {
        return new MatchPlayerDto(
            mp.getUser().getId(),
            mp.getUser().getDisplayName(),
            mp.getUser().getAvatarUrl(),
            mp.getTeamColor(),
            mp.getPlayerRole(),
            mp.getEloBefore(),
            mp.getEloAfter(),
            mp.getEloChange(),
            mp.getTeamElo(),
            mp.getWinProbability()
        );
    }
}
