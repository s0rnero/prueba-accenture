package com.accenture.franchises.repositories;

import com.accenture.franchises.entities.FranchiseEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface FranchiseRepository extends ReactiveCrudRepository<FranchiseEntity, UUID> {

    Flux<FranchiseEntity> findAllByOrderByNameAsc();
}
