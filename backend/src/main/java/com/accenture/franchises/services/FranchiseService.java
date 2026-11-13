package com.accenture.franchises.services;

import com.accenture.franchises.contracts.response.BranchResponse;
import com.accenture.franchises.contracts.response.FranchiseResponse;
import com.accenture.franchises.contracts.response.PageResponse;
import com.accenture.franchises.entities.BranchEntity;
import com.accenture.franchises.entities.FranchiseEntity;
import com.accenture.franchises.entities.ProductEntity;
import com.accenture.franchises.entities.UserFranchiseEntity;
import com.accenture.franchises.exceptions.ApiException;
import com.accenture.franchises.repositories.BranchRepository;
import com.accenture.franchises.repositories.FranchiseRepository;
import com.accenture.franchises.repositories.ProductRepository;
import com.accenture.franchises.repositories.UserFranchiseRepository;
import com.accenture.franchises.repositories.UserRepository;
import com.accenture.franchises.security.Actor;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class FranchiseService {

    public static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_NAME_LENGTH = 120;

    private final FranchiseRepository franchises;
    private final BranchRepository branches;
    private final ProductRepository products;
    private final UserFranchiseRepository scopes;
    private final UserRepository users;

    public FranchiseService(FranchiseRepository franchises, BranchRepository branches,
                            ProductRepository products, UserFranchiseRepository scopes,
                            UserRepository users) {
        this.franchises = franchises;
        this.branches = branches;
        this.products = products;
        this.scopes = scopes;
        this.users = users;
    }

    public Mono<PageResponse<FranchiseResponse>> page(int page, int size, Actor actor) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return visible(actor)
                .flatMap(this::withStats)
                .map(list -> {
                    int from = Math.min(safePage * safeSize, list.size());
                    int to = Math.min(from + safeSize, list.size());
                    return PageResponse.of(list.subList(from, to), safePage, safeSize, list.size());
                });
    }

    public Mono<FranchiseResponse> get(UUID id, Actor actor) {
        return ensureAccess(id, actor)
                .then(franchises.findById(id))
                .switchIfEmpty(Mono.error(notFound()))
                .flatMap(entity -> withStats(List.of(entity)).map(list -> list.get(0)));
    }

    public Mono<FranchiseResponse> create(String name) {
        String clean = name(name);
        Instant now = Instant.now();
        FranchiseEntity entity = new FranchiseEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(clean);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return franchises.save(entity)
                .flatMap(saved -> withStats(List.of(saved)).map(list -> list.get(0)));
    }

    public Mono<FranchiseResponse> rename(UUID id, String name, Integer version) {
        String clean = name(name);
        int expected = version(version);
        return franchises.findById(id)
                .switchIfEmpty(Mono.error(notFound()))
                .flatMap(entity -> {
                    if (!Integer.valueOf(expected).equals(entity.getVersion())) {
                        return Mono.error(conflict());
                    }
                    entity.setName(clean);
                    entity.setUpdatedAt(Instant.now());
                    return franchises.save(entity)
                            .onErrorMap(OptimisticLockingFailureException.class, error -> conflict());
                })
                .flatMap(saved -> withStats(List.of(saved)).map(list -> list.get(0)));
    }

    public Mono<Void> delete(UUID id) {
        return requireExisting(id).then(franchises.deleteById(id));
    }

    public Mono<List<BranchResponse>> branches(UUID franchiseId, Actor actor) {
        return ensureAccess(franchiseId, actor)
                .then(requireExisting(franchiseId))
                .then(branches.findByFranchiseIdOrderByNameAsc(franchiseId).collectList())
                .flatMap(list -> productsOf(list).map(items -> toBranchResponses(list, items)));
    }

    public Mono<BranchResponse> addBranch(UUID franchiseId, String name) {
        Instant now = Instant.now();
        BranchEntity branch = new BranchEntity();
        branch.setId(UUID.randomUUID());
        branch.setFranchiseId(franchiseId);
        branch.setName(name(name));
        branch.setCreatedAt(now);
        branch.setUpdatedAt(now);
        return requireExisting(franchiseId)
                .then(branches.save(branch))
                .map(saved -> new BranchResponse(saved.getId(), saved.getName(),
                        saved.getVersion(), 0));
    }

    public Mono<BranchResponse> renameBranch(UUID franchiseId, UUID branchId, String name,
                                             Integer version) {
        String clean = name(name);
        int expected = version(version);
        return branches.findByIdAndFranchiseId(branchId, franchiseId)
                .switchIfEmpty(Mono.error(ApiException.notFound("Sucursal no encontrada.")))
                .flatMap(branch -> {
                    if (!Integer.valueOf(expected).equals(branch.getVersion())) {
                        return Mono.error(conflict());
                    }
                    branch.setName(clean);
                    branch.setUpdatedAt(Instant.now());
                    return branches.save(branch)
                            .onErrorMap(OptimisticLockingFailureException.class, error -> conflict());
                })
                .flatMap(saved -> products.countByBranchId(saved.getId())
                        .map(count -> new BranchResponse(saved.getId(), saved.getName(),
                                saved.getVersion(), count)));
    }

    public Mono<Void> deleteBranch(UUID franchiseId, UUID branchId) {
        return branches.findByIdAndFranchiseId(branchId, franchiseId)
                .switchIfEmpty(Mono.error(ApiException.notFound("Sucursal no encontrada.")))
                .flatMap(branches::delete);
    }

    public Mono<Void> replaceMembers(UUID franchiseId, List<UUID> userIds) {
        List<UUID> targets = userIds == null ? List.of() : userIds;
        return requireExisting(franchiseId)
                .then(scopes.deleteByFranchiseId(franchiseId))
                .thenMany(Flux.fromIterable(targets)
                        .concatMap(userId -> users.existsById(userId)
                                .flatMap(exists -> exists
                                        ? scopes.save(new UserFranchiseEntity(null,
                                                userId, franchiseId)).then()
                                        : Mono.empty())))
                .then();
    }

    public Mono<Void> assignFirstFranchise(UUID userId) {
        return franchises.findAllByOrderByNameAsc().next()
                .flatMap(franchise -> scopes.save(new UserFranchiseEntity(null, userId,
                        franchise.getId())))
                .then();
    }

    public Mono<Void> requireExisting(UUID franchiseId) {
        return franchises.existsById(franchiseId)
                .flatMap(exists -> exists ? Mono.empty() : Mono.error(notFound()));
    }

    public Mono<Void> ensureAccess(UUID franchiseId, Actor actor) {
        if (actor.admin()) {
            return Mono.empty();
        }
        return scopes.existsByUserIdAndFranchiseId(actor.userId(), franchiseId)
                .flatMap(allowed -> Boolean.TRUE.equals(allowed)
                        ? Mono.empty()
                        : Mono.error(ApiException.forbidden("No tienes acceso a esta franquicia.")));
    }

    private Mono<List<FranchiseEntity>> visible(Actor actor) {
        if (actor.admin()) {
            return franchises.findAllByOrderByNameAsc().collectList();
        }
        return scopes.findByUserId(actor.userId())
                .map(UserFranchiseEntity::franchiseId)
                .collectList()
                .flatMap(ids -> ids.isEmpty()
                        ? Mono.just(List.<FranchiseEntity>of())
                        : franchises.findAllById(ids)
                                .sort(Comparator.comparing(FranchiseEntity::getName,
                                        String.CASE_INSENSITIVE_ORDER))
                                .collectList());
    }

    private Mono<List<FranchiseResponse>> withStats(List<FranchiseEntity> list) {
        if (list.isEmpty()) {
            return Mono.just(List.of());
        }
        List<UUID> ids = list.stream().map(FranchiseEntity::getId).toList();
        return branches.findByFranchiseIdIn(ids).collectList()
                .flatMap(branchList -> productsOf(branchList)
                        .map(items -> {
                            Map<UUID, List<BranchEntity>> branchesByFranchise =
                                    branchList.stream().collect(Collectors.groupingBy(
                                            BranchEntity::getFranchiseId));
                            Map<UUID, List<ProductEntity>> productsByBranch =
                                    items.stream().collect(Collectors.groupingBy(
                                            ProductEntity::getBranchId));
                            return list.stream()
                                    .map(franchise -> toResponse(franchise,
                                            branchesByFranchise.getOrDefault(franchise.getId(),
                                                    List.of()),
                                            productsByBranch))
                                    .toList();
                        }));
    }

    private Mono<List<ProductEntity>> productsOf(List<BranchEntity> branchList) {
        List<UUID> branchIds = branchList.stream().map(BranchEntity::getId).toList();
        return branchIds.isEmpty()
                ? Mono.just(List.of())
                : products.findByBranchIdIn(branchIds).collectList();
    }

    private List<BranchResponse> toBranchResponses(List<BranchEntity> list,
                                                   List<ProductEntity> items) {
        Map<UUID, Long> countByBranch = items.stream().collect(Collectors.groupingBy(
                ProductEntity::getBranchId, Collectors.counting()));
        return list.stream()
                .map(branch -> new BranchResponse(branch.getId(), branch.getName(),
                        branch.getVersion(), countByBranch.getOrDefault(branch.getId(), 0L)))
                .toList();
    }

    private FranchiseResponse toResponse(FranchiseEntity franchise, List<BranchEntity> branchList,
                                         Map<UUID, List<ProductEntity>> productsByBranch) {
        List<ProductEntity> owned = branchList.stream()
                .flatMap(branch -> productsByBranch.getOrDefault(branch.getId(), List.of())
                        .stream())
                .toList();
        long stock = owned.stream().mapToLong(ProductEntity::getStock).sum();
        return new FranchiseResponse(franchise.getId(), franchise.getName(),
                franchise.getVersion() == null ? 0 : franchise.getVersion(),
                branchList.size(), owned.size(), stock);
    }

    private String name(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            throw ApiException.badRequest("El nombre es obligatorio.");
        }
        if (clean.length() > MAX_NAME_LENGTH) {
            throw ApiException.badRequest("El nombre no puede superar " + MAX_NAME_LENGTH + " caracteres.");
        }
        return clean;
    }

    private int version(Integer value) {
        if (value == null || value < 0) {
            throw ApiException.badRequest("La version es obligatoria.");
        }
        return value;
    }

    private ApiException notFound() {
        return ApiException.notFound("Franquicia no encontrada.");
    }

    private ApiException conflict() {
        return ApiException.conflict("concurrent-modification",
                "El registro cambio desde que lo leiste. Recarga y vuelve a intentar.");
    }
}
