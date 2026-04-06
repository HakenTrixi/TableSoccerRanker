package com.tablesoccer.ranker.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 100) String username,
    @NotBlank @Size(min = 6) String password,
    @NotBlank @Size(max = 100) String displayName
) {}
