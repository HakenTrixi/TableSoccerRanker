package com.tablesoccer.ranker.admin;

import com.tablesoccer.ranker.user.Role;
import com.tablesoccer.ranker.user.User;

import java.util.UUID;

public record AdminUserDto(
    UUID id,
    String displayName,
    String email,
    String avatarUrl,
    Role role,
    int eloRating,
    int attackerElo,
    int defenderElo,
    boolean active,
    boolean hasGoogleLinked,
    boolean hasMatches
) {
    public static AdminUserDto from(User user, boolean hasMatches) {
        return new AdminUserDto(
            user.getId(),
            user.getDisplayName(),
            user.getEmail(),
            user.getAvatarUrl(),
            user.getRole(),
            user.getEloRating(),
            user.getAttackerElo(),
            user.getDefenderElo(),
            user.isActive(),
            user.getGoogleSub() != null,
            hasMatches
        );
    }
}
