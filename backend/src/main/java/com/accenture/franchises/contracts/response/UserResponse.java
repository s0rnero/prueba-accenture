package com.accenture.franchises.contracts.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        boolean enabled,
        List<String> roles,
        int version,
        Instant createdAt) {
}
