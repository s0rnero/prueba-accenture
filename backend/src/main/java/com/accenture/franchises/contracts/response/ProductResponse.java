package com.accenture.franchises.contracts.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID branchId,
        String branchName,
        String name,
        int stock,
        BigDecimal price,
        int version) {
}
