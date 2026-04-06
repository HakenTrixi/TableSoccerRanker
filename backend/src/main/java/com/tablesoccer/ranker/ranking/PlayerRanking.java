package com.tablesoccer.ranker.ranking;

import java.util.UUID;

public record PlayerRanking(
    int rank,
    UUID userId,
    String displayName,
    String avatarUrl,
    double score
) {}
