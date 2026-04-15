package com.tablesoccer.ranker.user;

import java.util.UUID;

public record UserDto(
    UUID id,
    String displayName,
    String email,
    String avatarUrl,
    Role role,
    int eloRating,
    boolean active
) {
    public static UserDto from(User user) {
        return new UserDto(
            user.getId(),
            user.getDisplayName(),
            user.getEmail(),
            user.getAvatarUrl(),
            user.getRole(),
            user.getEloRating(),
            user.isActive()
        );
    }
}
