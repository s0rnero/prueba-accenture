package com.accenture.franchises.repositories;

import com.accenture.franchises.entities.RoleEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RoleRepository extends ReactiveCrudRepository<RoleEntity, UUID> {

    Flux<RoleEntity> findAllByOrderByCodeAsc();

    Mono<RoleEntity> findByCode(String code);
}
