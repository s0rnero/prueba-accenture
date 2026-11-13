package com.accenture.franchises.contracts.response;

import java.util.List;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String email,
        String displayName,
        List<String> roles,
        List<String> permissions) {
}
