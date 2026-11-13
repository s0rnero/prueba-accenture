package com.accenture.franchises.contracts.request;

import java.math.BigDecimal;

public record ProductRequest(String name, Integer stock, BigDecimal price, Integer version) {
}
