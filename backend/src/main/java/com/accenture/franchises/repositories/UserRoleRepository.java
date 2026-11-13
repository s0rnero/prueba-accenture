package com.accenture.franchises.repositories;

import com.accenture.franchises.entities.UserRoleEntity;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRoleRepository extends ReactiveCrudRepository<UserRoleEntity, UUID> {

    Flux<UserRoleEntity> findByUserId(UUID userId);

    Flux<UserRoleEntity> findByUserIdIn(Collection<UUID> userIds);

    Mono<Void> deleteByUserId(UUID userId);
}
