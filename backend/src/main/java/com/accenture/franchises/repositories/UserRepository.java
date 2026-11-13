package com.accenture.franchises.repositories;

import com.accenture.franchises.entities.UserEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<UserEntity, UUID> {

    Mono<UserEntity> findByEmailIgnoreCase(String email);

    Flux<UserEntity> findAllByOrderByCreatedAtDesc();
}
