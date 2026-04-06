package com.tablesoccer.ranker.match;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record MatchCreateRequest(
    @NotNull UUID yellowAttacker,
    @NotNull UUID yellowDefender,
    @NotNull UUID whiteAttacker,
    @NotNull UUID whiteDefender,
    @Min(0) int yellowScore,
    @Min(0) int whiteScore,
    Instant playedAt
) {}
