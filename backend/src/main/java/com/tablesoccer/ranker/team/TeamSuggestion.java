package com.tablesoccer.ranker.team;

import com.tablesoccer.ranker.user.UserDto;

public record TeamSuggestion(
    UserDto yellowAttacker,
    UserDto yellowDefender,
    UserDto whiteAttacker,
    UserDto whiteDefender
) {}
