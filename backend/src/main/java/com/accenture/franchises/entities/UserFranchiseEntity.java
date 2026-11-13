package com.accenture.franchises.entities;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_franchises")
public record UserFranchiseEntity(
        @Id UUID id,
        @Column("user_id") UUID userId,
        @Column("franchise_id") UUID franchiseId) {
}
