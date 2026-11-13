package com.accenture.franchises.repositories;

import com.accenture.franchises.entities.PermissionEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PermissionRepository extends ReactiveCrudRepository<PermissionEntity, UUID> {
}
