package com.accenture.franchises.entities;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("permissions")
public record PermissionEntity(@Id UUID id, String code) {
}
