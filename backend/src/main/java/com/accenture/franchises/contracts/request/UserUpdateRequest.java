package com.accenture.franchises.contracts.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserUpdateRequest(
        @Size(max = 120) String displayName,
        Boolean enabled,
        List<String> roles,
        @NotNull Integer version) {
}
