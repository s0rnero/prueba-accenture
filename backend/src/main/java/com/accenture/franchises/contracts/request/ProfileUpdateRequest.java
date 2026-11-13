package com.accenture.franchises.contracts.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 120) String displayName,
        @Size(min = 8, max = 72)
        @Pattern(regexp = UserCreateRequest.PASSWORD_POLICY,
                message = UserCreateRequest.PASSWORD_MESSAGE) String password) {
}
