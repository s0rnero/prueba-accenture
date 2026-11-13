package com.accenture.franchises.entities;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("role_permissions")
public record RolePermissionEntity(
        @Id UUID id,
        @Column("role_id") UUID roleId,
        @Column("permission_id") UUID permissionId) {
}
