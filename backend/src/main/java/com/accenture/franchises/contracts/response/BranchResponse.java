package com.accenture.franchises.contracts.response;

import java.util.UUID;

public record BranchResponse(UUID id, String name, int version, long productCount) {
}
