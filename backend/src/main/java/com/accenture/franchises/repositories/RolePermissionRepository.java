package com.accenture.franchises.repositories;

import com.accenture.franchises.entities.RolePermissionEntity;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface RolePermissionRepository extends ReactiveCrudRepository<RolePermissionEntity, UUID> {

    Flux<RolePermissionEntity> findByRoleIdIn(Collection<UUID> roleIds);
}
