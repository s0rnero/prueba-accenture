package com.accenture.franchises.repositories;

import com.accenture.franchises.entities.UserFranchiseEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserFranchiseRepository extends ReactiveCrudRepository<UserFranchiseEntity, UUID> {

    Flux<UserFranchiseEntity> findByUserId(UUID userId);

    Mono<Boolean> existsByUserIdAndFranchiseId(UUID userId, UUID franchiseId);

    Mono<Void> deleteByFranchiseId(UUID franchiseId);
}
