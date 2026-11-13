package com.accenture.franchises.repositories;

import com.accenture.franchises.entities.ProductEntity;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductRepository extends ReactiveCrudRepository<ProductEntity, UUID> {

    Flux<ProductEntity> findByBranchIdIn(Collection<UUID> branchIds);

    Mono<ProductEntity> findByIdAndBranchId(UUID id, UUID branchId);

    Mono<Long> countByBranchId(UUID branchId);
}
