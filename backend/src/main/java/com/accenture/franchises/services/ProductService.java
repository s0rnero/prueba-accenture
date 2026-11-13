package com.accenture.franchises.services;

import com.accenture.franchises.contracts.response.PageResponse;
import com.accenture.franchises.contracts.response.ProductResponse;
import com.accenture.franchises.entities.BranchEntity;
import com.accenture.franchises.entities.ProductEntity;
import com.accenture.franchises.exceptions.ApiException;
import com.accenture.franchises.repositories.BranchRepository;
import com.accenture.franchises.repositories.ProductRepository;
import com.accenture.franchises.security.Actor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ProductService {

    private static final int MAX_NAME_LENGTH = 120;

    private final ProductRepository products;
    private final BranchRepository branches;
    private final FranchiseService franchises;

    public ProductService(ProductRepository products, BranchRepository branches,
                          FranchiseService franchises) {
        this.products = products;
        this.branches = branches;
        this.franchises = franchises;
    }

    @Cacheable(cacheNames = "products",
            key = "T(java.util.Objects).hash(#franchiseId, #page, #size, #search, #actor)")
    public Mono<PageResponse<ProductResponse>> page(UUID franchiseId, int page, int size,
                                                    String search, Actor actor) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), FranchiseService.MAX_PAGE_SIZE);
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return franchises.ensureAccess(franchiseId, actor)
                .then(franchises.requireExisting(franchiseId))
                .then(branches.findByFranchiseIdOrderByNameAsc(franchiseId).collectList())
                .flatMap(branchList -> {
                    Map<UUID, String> names = branchList.stream()
                            .collect(Collectors.toMap(BranchEntity::getId, BranchEntity::getName));
                    List<UUID> ids = branchList.stream().map(BranchEntity::getId).toList();
                    Mono<List<ProductEntity>> source = ids.isEmpty()
                            ? Mono.just(List.of())
                            : products.findByBranchIdIn(ids).collectList();
                    return source.map(items -> {
                        List<ProductResponse> all = items.stream()
                                .filter(product -> product.getName().toLowerCase(Locale.ROOT)
                                        .contains(term))
                                .sorted(Comparator
                                        .comparing((ProductEntity product) -> names
                                                .getOrDefault(product.getBranchId(), ""),
                                                String.CASE_INSENSITIVE_ORDER)
                                        .thenComparing(ProductEntity::getName,
                                                String.CASE_INSENSITIVE_ORDER))
                                .map(product -> toResponse(product, names))
                                .toList();
                        int from = Math.min(safePage * safeSize, all.size());
                        int to = Math.min(from + safeSize, all.size());
                        return PageResponse.of(all.subList(from, to), safePage, safeSize, all.size());
                    });
                });
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    public Mono<ProductResponse> create(UUID franchiseId, UUID branchId, String name,
                                        Integer stock, BigDecimal price) {
        String clean = name(name);
        int quantity = stock(stock);
        BigDecimal amount = price(price);
        return branches.findByIdAndFranchiseId(branchId, franchiseId)
                .switchIfEmpty(Mono.error(ApiException.notFound("Sucursal no encontrada.")))
                .flatMap(branch -> {
                    Instant now = Instant.now();
                    ProductEntity entity = new ProductEntity();
                    entity.setId(UUID.randomUUID());
                    entity.setBranchId(branchId);
                    entity.setName(clean);
                    entity.setStock(quantity);
                    entity.setPrice(amount);
                    entity.setCreatedAt(now);
                    entity.setUpdatedAt(now);
                    return products.save(entity)
                            .map(saved -> toResponse(saved, branch.getName()));
                });
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    public Mono<ProductResponse> update(UUID franchiseId, UUID branchId, UUID productId,
                                        String name, Integer stock, BigDecimal price,
                                        Integer version) {
        String clean = name(name);
        int quantity = stock(stock);
        BigDecimal amount = price(price);
        return owned(franchiseId, branchId, productId)
                .flatMap(existing -> change(existing, version, entity -> {
                    entity.setName(clean);
                    entity.setStock(quantity);
                    entity.setPrice(amount);
                }));
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    public Mono<ProductResponse> updateStock(UUID franchiseId, UUID branchId, UUID productId,
                                             Integer stock, Integer version) {
        int quantity = stock(stock);
        return owned(franchiseId, branchId, productId)
                .flatMap(existing -> change(existing, version,
                        entity -> entity.setStock(quantity)));
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    public Mono<Void> delete(UUID franchiseId, UUID branchId, UUID productId) {
        return owned(franchiseId, branchId, productId).flatMap(products::delete);
    }

    private Mono<ProductEntity> owned(UUID franchiseId, UUID branchId, UUID productId) {
        return branches.findByIdAndFranchiseId(branchId, franchiseId)
                .switchIfEmpty(Mono.error(ApiException.notFound("Producto no encontrado.")))
                .then(products.findByIdAndBranchId(productId, branchId))
                .switchIfEmpty(Mono.error(ApiException.notFound("Producto no encontrado.")));
    }

    private Mono<ProductResponse> change(ProductEntity existing, Integer version,
                                         Consumer<ProductEntity> mutation) {
        if (!version(version).equals(existing.getVersion())) {
            return Mono.error(conflict());
        }
        mutation.accept(existing);
        existing.setUpdatedAt(Instant.now());
        return products.save(existing)
                .onErrorMap(OptimisticLockingFailureException.class, error -> conflict())
                .flatMap(saved -> branches.findById(saved.getBranchId())
                        .map(branch -> toResponse(saved, branch.getName())));
    }

    private ProductResponse toResponse(ProductEntity entity, Map<UUID, String> branchNames) {
        return toResponse(entity, branchNames.getOrDefault(entity.getBranchId(), ""));
    }

    private ProductResponse toResponse(ProductEntity entity, String branchName) {
        return new ProductResponse(entity.getId(), entity.getBranchId(), branchName,
                entity.getName(), entity.getStock(), entity.getPrice(),
                entity.getVersion() == null ? 0 : entity.getVersion());
    }

    private String name(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            throw ApiException.badRequest("El nombre del producto es obligatorio.");
        }
        if (clean.length() > MAX_NAME_LENGTH) {
            throw ApiException.badRequest("El nombre no puede superar " + MAX_NAME_LENGTH + " caracteres.");
        }
        return clean;
    }

    private int stock(Integer value) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw ApiException.badRequest("El stock no puede ser negativo.");
        }
        return value;
    }

    private BigDecimal price(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (value.signum() < 0) {
            throw ApiException.badRequest("El precio no puede ser negativo.");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Integer version(Integer value) {
        if (value == null || value < 0) {
            throw ApiException.badRequest("La version es obligatoria.");
        }
        return value;
    }

    private ApiException conflict() {
        return ApiException.conflict("concurrent-modification",
                "El producto cambio desde que lo leiste. Recarga y vuelve a intentar.");
    }
}
