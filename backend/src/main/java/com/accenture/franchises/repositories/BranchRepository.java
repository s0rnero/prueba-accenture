package com.accenture.franchises.repositories;

import com.accenture.franchises.entities.BranchEntity;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BranchRepository extends ReactiveCrudRepository<BranchEntity, UUID> {

    Flux<BranchEntity> findByFranchiseIdOrderByNameAsc(UUID franchiseId);

    Flux<BranchEntity> findByFranchiseIdIn(Collection<UUID> franchiseIds);

    Mono<BranchEntity> findByIdAndFranchiseId(UUID id, UUID franchiseId);

    Mono<Long> countByFranchiseId(UUID franchiseId);
}
