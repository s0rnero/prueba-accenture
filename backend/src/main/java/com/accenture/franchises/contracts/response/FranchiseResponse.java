package com.accenture.franchises.contracts.response;

import java.util.UUID;

public record FranchiseResponse(
        UUID id,
        String name,
        int version,
        long branchCount,
        long productCount,
        long totalStock) {
}
