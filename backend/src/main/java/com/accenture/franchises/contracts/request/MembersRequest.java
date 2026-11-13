package com.accenture.franchises.contracts.request;

import java.util.List;
import java.util.UUID;

public record MembersRequest(List<UUID> userIds) {
}
